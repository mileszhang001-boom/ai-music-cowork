#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
LLM 批量分析工具
使用 Qwen API 批量分析音乐标签，支持并发、进度跟踪、断点续传。
"""

import argparse
import asyncio
import json
import logging
import os
import re
import sys
import time
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple, TYPE_CHECKING

if TYPE_CHECKING:
    import aiohttp

try:
    import aiohttp
    AIOHTTP_AVAILABLE = True
except ImportError:
    AIOHTTP_AVAILABLE = False
    aiohttp = None  # type: ignore

try:
    from tqdm import tqdm
    TQDM_AVAILABLE = True
except ImportError:
    TQDM_AVAILABLE = False


API_BASE = "https://dashscope.aliyuncs.com/compatible-mode/v1"
MODEL_NAME = "qwen-plus"
DEFAULT_CONCURRENCY = 8
SAVE_INTERVAL = 50
MAX_RETRIES = 3
RETRY_DELAY = 2


@dataclass
class AnalysisResult:
    id: int
    title: str
    artist: str
    success: bool = False
    tags: Optional[Dict[str, Any]] = None
    error: Optional[str] = None
    retry_count: int = 0


@dataclass
class ProgressTracker:
    total: int = 0
    completed: int = 0
    failed: int = 0
    start_time: float = field(default_factory=time.time)
    
    def eta(self) -> str:
        if self.completed == 0:
            return "计算中..."
        elapsed = time.time() - self.start_time
        avg_time = elapsed / self.completed
        remaining = (self.total - self.completed) * avg_time
        if remaining < 60:
            return f"{int(remaining)}秒"
        elif remaining < 3600:
            return f"{int(remaining / 60)}分钟"
        else:
            return f"{remaining / 3600:.1f}小时"
    
    def speed(self) -> str:
        elapsed = time.time() - self.start_time
        if elapsed == 0:
            return "0.0 首/秒"
        return f"{self.completed / elapsed:.2f} 首/秒"


class LLMAnalyzer:
    def __init__(
        self,
        api_key: str,
        concurrency: int = DEFAULT_CONCURRENCY,
        prompt_template_path: Optional[str] = None,
        partial_output_path: Optional[str] = None,
        error_log_path: Optional[str] = None,
    ):
        if not AIOHTTP_AVAILABLE:
            raise ImportError("请安装 aiohttp: pip install aiohttp")
        
        self.api_key = api_key
        self.concurrency = min(max(concurrency, 5), 10)
        self.prompt_template = self._load_prompt_template(prompt_template_path)
        self.partial_output_path = partial_output_path
        self.error_log_path = error_log_path
        self.results: Dict[int, Dict[str, Any]] = {}
        self.failed_ids: List[int] = []
        self.progress = ProgressTracker()
        self.pbar = None
        
        self._setup_logging()
    
    def _setup_logging(self):
        self.logger = logging.getLogger("llm_analyzer")
        self.logger.setLevel(logging.INFO)
        
        if not self.logger.handlers:
            console_handler = logging.StreamHandler()
            console_handler.setLevel(logging.INFO)
            formatter = logging.Formatter(
                "%(asctime)s [%(levelname)s] %(message)s",
                datefmt="%Y-%m-%d %H:%M:%S"
            )
            console_handler.setFormatter(formatter)
            self.logger.addHandler(console_handler)
        
        if self.error_log_path:
            file_handler = logging.FileHandler(self.error_log_path, encoding="utf-8")
            file_handler.setLevel(logging.ERROR)
            file_handler.setFormatter(formatter)
            self.logger.addHandler(file_handler)
    
    def _load_prompt_template(self, template_path: Optional[str]) -> str:
        default_template = """你是一位专业的音乐分析师，擅长分析歌曲的特征和标签。你的任务是根据歌曲的基本信息，推断出标准化的音乐标签，用于音乐推荐系统的场景匹配。

你需要输出符合以下规范的 JSON 格式数据：
- 所有数值字段必须在指定范围内
- 标签字段必须从预定义的枚举值中选择
- 输出必须是合法的 JSON 格式，不要包含任何其他文字说明"""
        
        if not template_path:
            return default_template
        
        template_file = Path(template_path)
        if not template_file.exists():
            self.logger.warning(f"提示词模板文件不存在: {template_path}，使用默认模板")
            return default_template
        
        try:
            with open(template_file, "r", encoding="utf-8") as f:
                content = f.read()
            
            system_match = re.search(r"## 系统提示词.*?\n(.+?)(?=\n## |$)", content, re.DOTALL)
            if system_match:
                return system_match.group(1).strip()
            
            return default_template
        except Exception as e:
            self.logger.warning(f"读取提示词模板失败: {e}，使用默认模板")
            return default_template
    
    def _build_user_prompt(self, track: Dict[str, Any]) -> str:
        title = track.get("title", "未知")
        artist = track.get("artist", "未知艺术家")
        album = track.get("album") or "未知专辑"
        genre = track.get("genre") or "未知"
        duration_ms = track.get("duration_ms", 0)
        duration_sec = duration_ms // 1000 if duration_ms else 0
        
        language = "zh" if any("\u4e00" <= c <= "\u9fff" for c in title + artist) else "en"
        if "instrumental" in title.lower() or "纯音乐" in title:
            language = "instrumental"
        
        return f"""请分析以下歌曲的音乐特征，并生成标准化的标签数据。

### 歌曲信息
- 歌名: {title}
- 歌手: {artist}
- 专辑: {album}
- 原有流派: {genre}
- 时长: {duration_sec} 秒
- 语言: {language}

### 分析要求

请根据歌名、歌手、专辑名称等信息，推断以下标签：

#### 1. 主要流派 (genre) [必填]
从以下选项中选择一个最匹配的流派：
- pop (流行)
- rock (摇滚)
- jazz (爵士)
- classical (古典)
- electronic (电子)
- folk (民谣)
- rnb (节奏布鲁斯)
- hip_hop (嘻哈)
- country (乡村)
- ambient (氛围音乐)
- blues (蓝调)
- metal (金属)
- punk (朋克)
- reggae (雷鬼)
- latin (拉丁)
- indie (独立)
- soul (灵魂乐)
- funk (放克)
- disco (迪斯科)
- lofi (低保真)
- ballad (抒情)
- acoustic (原声)
- soundtrack (原声带)
- world (世界音乐)
- children (儿童音乐)
- religious (宗教音乐)
- other (其他)

#### 2. 节拍速度 (bpm) [可选]
估算歌曲的每分钟节拍数，范围: 40-220

#### 3. 能量值 (energy) [必填]
衡量歌曲的活力和强度，范围: 0.0-1.0

#### 4. 情绪正向度 (valence) [必填]
衡量歌曲传递的积极/消极情绪，范围: 0.0-1.0

#### 5. 可舞性 (danceability) [可选]
衡量歌曲是否适合跳舞，范围: 0.0-1.0

#### 6. 原声程度 (acousticness) [可选]
衡量歌曲是否为原声乐器演奏，范围: 0.0-1.0

#### 7. 纯音乐程度 (instrumentalness) [可选]
衡量歌曲是否无人声，范围: 0.0-1.0

#### 8. 情绪标签 (mood_tags) [必填，1-5个]
从以下选项中选择 1-5 个最匹配的情绪标签：
happy, sad, energetic, calm, romantic, melancholy, peaceful, excited, 
nostalgic, hopeful, dark, bright, dreamy, aggressive, relaxing, 
uplifting, sentimental, playful, mysterious, epic

#### 9. 活动标签 (activity_tags) [可选，0-5个]
从以下选项中选择适合的活动场景：
driving, working, exercise, relax, party, sleep, study, cooking, 
cleaning, gaming, meditation, yoga, running, walking, commute

#### 10. 天气标签 (weather_tags) [可选，0-3个]
从以下选项中选择适合的天气场景：
sunny, rainy, cloudy, snowy, windy, stormy, foggy, clear_night

#### 11. 时间标签 (time_tags) [可选，0-3个]
从以下选项中选择适合的时间段：
morning, afternoon, evening, night, late_night, dawn, dusk

#### 12. 场景标签 (scene_tags) [可选，0-5个]
从以下选项中选择适合的场景：
morning_commute, night_drive, road_trip, party, romantic_date, 
focus_work, relax, workout, sleep, meditation, rainy_day, sunny_day, 
winter, summer, family_trip, city, highway, home, office, cafe, outdoor

### 输出格式

请严格按照以下 JSON 格式输出，不要添加任何其他文字：

```json
{{
  "genre": "流派",
  "bpm": 节拍数,
  "energy": 能量值,
  "valence": 情绪正向度,
  "danceability": 可舞性,
  "acousticness": 原声程度,
  "instrumentalness": 纯音乐程度,
  "mood_tags": ["情绪标签1", "情绪标签2"],
  "activity_tags": ["活动标签1", "活动标签2"],
  "weather_tags": ["天气标签"],
  "time_tags": ["时间标签"],
  "scene_tags": ["场景标签1", "场景标签2"]
}}
```"""
    
    def _parse_llm_response(self, content: str) -> Optional[Dict[str, Any]]:
        try:
            json_str = content
            json_str = re.sub(r"```json\s*", "", json_str)
            json_str = re.sub(r"```\s*", "", json_str)
            json_str = json_str.strip()
            
            json_match = re.search(r"\{[\s\S]*\}", json_str)
            if json_match:
                json_str = json_match.group(0)
            
            tags = json.loads(json_str)
            
            if "genre" not in tags:
                return None
            
            valid_genres = {
                "pop", "rock", "jazz", "classical", "electronic", "folk", "rnb",
                "hip_hop", "country", "ambient", "blues", "metal", "punk",
                "reggae", "latin", "indie", "soul", "funk", "disco", "lofi",
                "ballad", "acoustic", "soundtrack", "world", "children",
                "religious", "other"
            }
            if tags.get("genre") not in valid_genres:
                tags["genre"] = "other"
            
            for num_field in ["bpm", "energy", "valence", "danceability", "acousticness", "instrumentalness"]:
                if num_field in tags and tags[num_field] is not None:
                    try:
                        tags[num_field] = float(tags[num_field])
                        if num_field == "bpm":
                            tags[num_field] = max(40, min(220, int(tags[num_field])))
                        else:
                            tags[num_field] = max(0.0, min(1.0, tags[num_field]))
                    except (ValueError, TypeError):
                        tags[num_field] = None
            
            valid_moods = {
                "happy", "sad", "energetic", "calm", "romantic", "melancholy",
                "peaceful", "excited", "nostalgic", "hopeful", "dark", "bright",
                "dreamy", "aggressive", "relaxing", "uplifting", "sentimental",
                "playful", "mysterious", "epic"
            }
            for tag_field in ["mood_tags", "activity_tags", "weather_tags", "time_tags", "scene_tags"]:
                if tag_field in tags and isinstance(tags[tag_field], list):
                    tags[tag_field] = [t for t in tags[tag_field] if isinstance(t, str)]
                    if tag_field == "mood_tags":
                        tags[tag_field] = [t for t in tags[tag_field] if t in valid_moods]
            
            return tags
        except json.JSONDecodeError as e:
            self.logger.error(f"JSON 解析失败: {e}")
            return None
        except Exception as e:
            self.logger.error(f"解析 LLM 响应失败: {e}")
            return None
    
    async def _call_qwen_api(
        self,
        session: "aiohttp.ClientSession",
        track: Dict[str, Any],
        retry_count: int = 0
    ) -> AnalysisResult:
        track_id = track.get("id", 0)
        title = track.get("title", "未知")
        artist = track.get("artist", "未知艺术家")
        
        user_prompt = self._build_user_prompt(track)
        
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }
        
        data = {
            "model": MODEL_NAME,
            "messages": [
                {"role": "system", "content": self.prompt_template},
                {"role": "user", "content": user_prompt}
            ],
            "temperature": 0.3,
            "max_tokens": 800
        }
        
        try:
            async with session.post(
                f"{API_BASE}/chat/completions",
                headers=headers,
                json=data,
                timeout=aiohttp.ClientTimeout(total=60)
            ) as response:
                if response.status == 200:
                    result = await response.json()
                    content = result["choices"][0]["message"]["content"]
                    tags = self._parse_llm_response(content)
                    
                    if tags:
                        return AnalysisResult(
                            id=track_id,
                            title=title,
                            artist=artist,
                            success=True,
                            tags=tags,
                            retry_count=retry_count
                        )
                    else:
                        return AnalysisResult(
                            id=track_id,
                            title=title,
                            artist=artist,
                            success=False,
                            error="无法解析 LLM 响应",
                            retry_count=retry_count
                        )
                elif response.status == 429:
                    retry_after = int(response.headers.get("Retry-After", RETRY_DELAY * 2))
                    self.logger.warning(f"API 限流，等待 {retry_after} 秒后重试")
                    await asyncio.sleep(retry_after)
                    return await self._call_qwen_api(session, track, retry_count + 1)
                else:
                    error_text = await response.text()
                    return AnalysisResult(
                        id=track_id,
                        title=title,
                        artist=artist,
                        success=False,
                        error=f"API 错误 {response.status}: {error_text[:200]}",
                        retry_count=retry_count
                    )
        except asyncio.TimeoutError:
            return AnalysisResult(
                id=track_id,
                title=title,
                artist=artist,
                success=False,
                error="请求超时",
                retry_count=retry_count
            )
        except aiohttp.ClientError as e:
            return AnalysisResult(
                id=track_id,
                title=title,
                artist=artist,
                success=False,
                error=f"网络错误: {str(e)}",
                retry_count=retry_count
            )
        except Exception as e:
            return AnalysisResult(
                id=track_id,
                title=title,
                artist=artist,
                success=False,
                error=f"未知错误: {str(e)}",
                retry_count=retry_count
            )
    
    async def _analyze_with_retry(
        self,
        session: "aiohttp.ClientSession",
        track: Dict[str, Any],
        semaphore: asyncio.Semaphore
    ) -> AnalysisResult:
        async with semaphore:
            result = await self._call_qwen_api(session, track)
            
            if not result.success and result.retry_count < MAX_RETRIES:
                for attempt in range(MAX_RETRIES - result.retry_count):
                    await asyncio.sleep(RETRY_DELAY * (attempt + 1))
                    result = await self._call_qwen_api(session, track, result.retry_count + attempt + 1)
                    if result.success:
                        break
            
            return result
    
    def _update_progress(self, result: AnalysisResult):
        self.progress.completed += 1
        
        if result.success:
            self.results[result.id] = result.tags
        else:
            self.progress.failed += 1
            self.failed_ids.append(result.id)
            self.logger.error(
                f"分析失败 [ID:{result.id}] {result.title} - {result.artist}: {result.error}"
            )
        
        if self.pbar:
            self.pbar.update(1)
            self.pbar.set_postfix({
                "成功": self.progress.completed - self.progress.failed,
                "失败": self.progress.failed,
                "速度": self.progress.speed()
            })
        
        if self.progress.completed % SAVE_INTERVAL == 0:
            self._save_partial_results()
    
    def _save_partial_results(self):
        if not self.partial_output_path:
            return
        
        try:
            output_path = Path(self.partial_output_path)
            output_path.parent.mkdir(parents=True, exist_ok=True)
            
            with open(output_path, "w", encoding="utf-8") as f:
                json.dump(self.results, f, ensure_ascii=False, indent=2)
            
            self.logger.info(
                f"已保存中间结果: {len(self.results)} 首 (进度: {self.progress.completed}/{self.progress.total})"
            )
        except Exception as e:
            self.logger.error(f"保存中间结果失败: {e}")
    
    def _load_partial_results(self) -> Dict[int, Dict[str, Any]]:
        if not self.partial_output_path:
            return {}
        
        partial_path = Path(self.partial_output_path)
        if not partial_path.exists():
            return {}
        
        try:
            with open(partial_path, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            self.logger.warning(f"加载中间结果失败: {e}")
            return {}
    
    async def analyze_batch(
        self,
        tracks: List[Dict[str, Any]],
        start_id: Optional[int] = None
    ) -> Dict[str, Dict[str, Any]]:
        existing_results = self._load_partial_results()
        self.results = existing_results
        
        if start_id is not None:
            tracks_to_analyze = [t for t in tracks if t.get("id", 0) >= start_id]
        else:
            tracks_to_analyze = [
                t for t in tracks
                if t.get("id") not in self.results
            ]
        
        if not tracks_to_analyze:
            self.logger.info("所有歌曲已分析完成，无需处理")
            return self.results
        
        self.progress.total = len(tracks_to_analyze)
        self.logger.info(
            f"开始分析 {len(tracks_to_analyze)} 首歌曲 "
            f"(并发数: {self.concurrency}, 已完成: {len(self.results)})"
        )
        
        semaphore = asyncio.Semaphore(self.concurrency)
        
        connector = aiohttp.TCPConnector(limit=self.concurrency, limit_per_host=self.concurrency)
        
        if TQDM_AVAILABLE:
            self.pbar = tqdm(
                total=len(tracks_to_analyze),
                desc="分析进度",
                unit="首",
                ncols=100
            )
        else:
            self.pbar = None
            print(f"开始处理 {len(tracks_to_analyze)} 首歌曲...")
        
        async with aiohttp.ClientSession(connector=connector) as session:
            tasks = [
                self._analyze_with_retry(session, track, semaphore)
                for track in tracks_to_analyze
            ]
            
            for coro in asyncio.as_completed(tasks):
                result = await coro
                self._update_progress(result)
        
        if self.pbar:
            self.pbar.close()
        
        self._save_partial_results()
        
        self.logger.info(
            f"\n分析完成:\n"
            f"  成功: {self.progress.completed - self.progress.failed}\n"
            f"  失败: {self.progress.failed}\n"
            f"  总耗时: {time.time() - self.progress.start_time:.1f}秒\n"
            f"  平均速度: {self.progress.speed()}"
        )
        
        if self.failed_ids:
            self.logger.warning(f"失败的歌曲 ID: {self.failed_ids[:20]}{'...' if len(self.failed_ids) > 20 else ''}")
        
        return self.results


def load_index(index_path: str) -> List[Dict[str, Any]]:
    with open(index_path, "r", encoding="utf-8") as f:
        return json.load(f)


def merge_results(
    tracks: List[Dict[str, Any]],
    results: Dict[int, Dict[str, Any]]
) -> List[Dict[str, Any]]:
    merged = []
    for track in tracks:
        track_id = track.get("id")
        if str(track_id) in results or track_id in results:
            tags = results.get(track_id) or results.get(str(track_id))
            if tags:
                merged_track = track.copy()
                merged_track.update({
                    "genre": tags.get("genre", track.get("genre")),
                    "bpm": tags.get("bpm"),
                    "energy": tags.get("energy"),
                    "valence": tags.get("valence"),
                    "danceability": tags.get("danceability"),
                    "acousticness": tags.get("acousticness"),
                    "instrumentalness": tags.get("instrumentalness"),
                    "mood_tags": json.dumps(tags.get("mood_tags", []), ensure_ascii=False) if tags.get("mood_tags") else None,
                    "activity_tags": json.dumps(tags.get("activity_tags", []), ensure_ascii=False) if tags.get("activity_tags") else None,
                    "weather_tags": json.dumps(tags.get("weather_tags", []), ensure_ascii=False) if tags.get("weather_tags") else None,
                    "time_tags": json.dumps(tags.get("time_tags", []), ensure_ascii=False) if tags.get("time_tags") else None,
                    "scene_tags": json.dumps(tags.get("scene_tags", []), ensure_ascii=False) if tags.get("scene_tags") else None,
                    "llm_analyzed": 1
                })
                merged.append(merged_track)
            else:
                merged.append(track)
        else:
            merged.append(track)
    return merged


async def main_async(args):
    api_key = args.api_key or os.environ.get("DASHSCOPE_API_KEY")
    if not api_key:
        print("错误: 请设置 DASHSCOPE_API_KEY 环境变量或使用 --api-key 参数")
        sys.exit(1)
    
    index_path = Path(args.index)
    if not index_path.exists():
        print(f"错误: 索引文件不存在: {index_path}")
        sys.exit(1)
    
    print(f"加载索引文件: {index_path}")
    tracks = load_index(str(index_path))
    print(f"共加载 {len(tracks)} 首歌曲")
    
    analyzer = LLMAnalyzer(
        api_key=api_key,
        concurrency=args.concurrency,
        prompt_template_path=args.prompt_template,
        partial_output_path=args.partial_output,
        error_log_path=args.error_log
    )
    
    results = await analyzer.analyze_batch(tracks, start_id=args.start_id)
    
    if args.output:
        merged_tracks = merge_results(tracks, results)
        output_path = Path(args.output)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(merged_tracks, f, ensure_ascii=False, indent=2)
        
        print(f"\n结果已保存到: {output_path}")


def main():
    parser = argparse.ArgumentParser(
        description="LLM 批量分析工具 - 使用 Qwen API 分析音乐标签",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例用法:
  # 分析所有歌曲
  python llm_analyzer.py -i data/index.json -o data/index_analyzed.json

  # 从指定 ID 开始继续分析
  python llm_analyzer.py -i data/index.json --start-id 100

  # 设置并发数为 10
  python llm_analyzer.py -i data/index.json -c 10

环境变量:
  DASHSCOPE_API_KEY  阿里云百炼 API Key
        """
    )
    
    parser.add_argument(
        "-i", "--index",
        required=True,
        help="输入索引文件路径 (JSON 格式)"
    )
    parser.add_argument(
        "-o", "--output",
        help="输出文件路径 (合并后的完整索引)"
    )
    parser.add_argument(
        "--api-key",
        help="阿里云百炼 API Key (或设置 DASHSCOPE_API_KEY 环境变量)"
    )
    parser.add_argument(
        "-c", "--concurrency",
        type=int,
        default=DEFAULT_CONCURRENCY,
        help=f"并发请求数 (5-10, 默认: {DEFAULT_CONCURRENCY})"
    )
    parser.add_argument(
        "--start-id",
        type=int,
        help="从指定 ID 开始继续处理"
    )
    parser.add_argument(
        "--prompt-template",
        default="/Users/bella/ai-music-cowork/tools/music-organizer/llm_prompt_template.txt",
        help="提示词模板文件路径"
    )
    parser.add_argument(
        "--partial-output",
        default="/Users/bella/ai-music-cowork/data/index_partial.json",
        help="中间结果保存路径"
    )
    parser.add_argument(
        "--error-log",
        default="/Users/bella/ai-music-cowork/data/analysis_errors.log",
        help="错误日志保存路径"
    )
    
    args = parser.parse_args()
    
    asyncio.run(main_async(args))


if __name__ == "__main__":
    main()
