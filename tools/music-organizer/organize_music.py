#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PC端音乐元数据提取工具
用于提取音乐文件的元数据并生成 JSON 索引文件，支持 LLM 分析音乐标签。
"""

import argparse
import json
import os
import re
import sys
import time
import uuid
from pathlib import Path
from typing import Optional, Dict, Any, List

try:
    from mutagen import File as MutagenFile
    from mutagen.flac import FLAC
    from mutagen.mp3 import MP3
    from mutagen.m4a import M4A
    from mutagen.oggvorbis import OggVorbis
    from mutagen.wave import WAVE
except ImportError:
    print("错误: 请先安装 mutagen 库: pip install mutagen")
    sys.exit(1)

try:
    from pypinyin import lazy_pinyin, Style
    PINYIN_AVAILABLE = True
except ImportError:
    PINYIN_AVAILABLE = False

try:
    import requests
    REQUESTS_AVAILABLE = True
except ImportError:
    REQUESTS_AVAILABLE = False

SUPPORTED_FORMATS = {'.mp3', '.flac', '.wav', '.m4a', '.ogg'}

GENRE_LIST = [
    'pop', 'rock', 'jazz', 'classical', 'electronic', 'folk', 'rnb',
    'hip_hop', 'country', 'ambient', 'blues', 'metal', 'punk',
    'reggae', 'latin', 'indie', 'soul', 'funk', 'disco', 'lofi',
    'ballad', 'acoustic', 'soundtrack', 'world', 'other'
]

MOOD_LIST = [
    'happy', 'sad', 'energetic', 'calm', 'romantic', 'melancholy',
    'peaceful', 'excited', 'nostalgic', 'hopeful', 'dark', 'bright',
    'dreamy', 'aggressive', 'relaxing', 'uplifting', 'sentimental', 'playful'
]

SCENE_LIST = [
    'morning_commute', 'night_drive', 'road_trip', 'party', 'romantic_date',
    'focus_work', 'relax', 'workout', 'sleep', 'meditation',
    'rainy_day', 'sunny_day', 'winter', 'summer', 'family_trip'
]


def to_pinyin(text: str) -> Optional[str]:
    if not text or not PINYIN_AVAILABLE:
        return None
    try:
        pinyin_list = lazy_pinyin(text, style=Style.NORMAL)
        return ' '.join(pinyin_list)
    except Exception:
        return None


def parse_filename(file_path: Path) -> Dict[str, Optional[str]]:
    result = {'artist': None, 'title': None}
    stem = file_path.stem
    
    patterns = [
        r'^(.+?)\s*-\s*(.+)$',
        r'^(.+?)\s*–\s*(.+)$',
        r'^(.+?)\s*—\s*(.+)$',
    ]
    
    for pattern in patterns:
        match = re.match(pattern, stem)
        if match:
            result['artist'] = match.group(1).strip()
            result['title'] = match.group(2).strip()
            break
    
    if not result['title']:
        result['title'] = stem
    
    return result


def get_audio_metadata(file_path: Path) -> Optional[Dict[str, Any]]:
    try:
        audio = MutagenFile(str(file_path), easy=True)
        if audio is None:
            return None
        
        metadata = {
            'title': None,
            'artist': None,
            'album': None,
            'genre': None,
            'year': None,
            'duration_ms': None,
            'bitrate': None,
            'sample_rate': None,
            'format': file_path.suffix[1:].upper(),
            'file_path': str(file_path),
            'file_size': file_path.stat().st_size,
        }
        
        if hasattr(audio, 'info'):
            info = audio.info
            if hasattr(info, 'length'):
                metadata['duration_ms'] = int(info.length * 1000)
            if hasattr(info, 'bitrate'):
                metadata['bitrate'] = info.bitrate
            if hasattr(info, 'sample_rate'):
                metadata['sample_rate'] = info.sample_rate
        
        if file_path.suffix.lower() == '.flac':
            audio_full = FLAC(str(file_path))
            metadata['title'] = audio_full.get('title', [None])[0]
            metadata['artist'] = audio_full.get('artist', [None])[0]
            metadata['album'] = audio_full.get('album', [None])[0]
            metadata['genre'] = audio_full.get('genre', [None])[0]
            metadata['year'] = audio_full.get('date', [None])[0]
        elif file_path.suffix.lower() == '.mp3':
            audio_full = MP3(str(file_path))
            if audio_full.tags:
                metadata['title'] = str(audio_full.tags.get('TIT2', [''])[0]) if audio_full.tags.get('TIT2') else None
                metadata['artist'] = str(audio_full.tags.get('TPE1', [''])[0]) if audio_full.tags.get('TPE1') else None
                metadata['album'] = str(audio_full.tags.get('TALB', [''])[0]) if audio_full.tags.get('TALB') else None
                metadata['genre'] = str(audio_full.tags.get('TCON', [''])[0]) if audio_full.tags.get('TCON') else None
                metadata['year'] = str(audio_full.tags.get('TDRC', [''])[0]) if audio_full.tags.get('TDRC') else None
        elif file_path.suffix.lower() == '.m4a':
            audio_full = M4A(str(file_path))
            metadata['title'] = audio_full.get('\xa9nam', [None])[0]
            metadata['artist'] = audio_full.get('\xa9ART', [None])[0]
            metadata['album'] = audio_full.get('\xa9alb', [None])[0]
            metadata['genre'] = audio_full.get('\xa9gen', [None])[0]
            metadata['year'] = audio_full.get('\xa9day', [None])[0]
        elif file_path.suffix.lower() == '.ogg':
            audio_full = OggVorbis(str(file_path))
            metadata['title'] = audio_full.get('title', [None])[0]
            metadata['artist'] = audio_full.get('artist', [None])[0]
            metadata['album'] = audio_full.get('album', [None])[0]
            metadata['genre'] = audio_full.get('genre', [None])[0]
            metadata['year'] = audio_full.get('date', [None])[0]
        elif file_path.suffix.lower() == '.wav':
            try:
                audio_full = WAVE(str(file_path))
                if hasattr(audio_full, 'tags') and audio_full.tags:
                    metadata['title'] = audio_full.tags.get('Title', [None])[0] if 'Title' in audio_full.tags else None
                    metadata['artist'] = audio_full.tags.get('Artist', [None])[0] if 'Artist' in audio_full.tags else None
            except Exception:
                pass
        else:
            metadata['title'] = audio.get('title', [None])[0]
            metadata['artist'] = audio.get('artist', [None])[0]
            metadata['album'] = audio.get('album', [None])[0]
            metadata['genre'] = audio.get('genre', [None])[0]
        
        parsed = parse_filename(file_path)
        if not metadata['title']:
            metadata['title'] = parsed['title'] or file_path.stem
        if not metadata['artist']:
            metadata['artist'] = parsed['artist'] or '未知艺术家'
        
        metadata['title_pinyin'] = to_pinyin(metadata['title'])
        metadata['artist_pinyin'] = to_pinyin(metadata['artist'])
        
        return metadata
    
    except Exception as e:
        print(f"警告: 无法读取文件 {file_path}: {e}")
        return None


def analyze_with_llm(metadata: Dict[str, Any], api_key: str, api_base: str = "https://dashscope.aliyuncs.com/compatible-mode/v1") -> Optional[Dict[str, Any]]:
    if not REQUESTS_AVAILABLE or not api_key:
        return None
    
    title = metadata.get('title', '')
    artist = metadata.get('artist', '')
    album = metadata.get('album', '')
    genre = metadata.get('genre', '')
    duration = metadata.get('duration_ms', 0)
    duration_sec = duration // 1000 if duration else 0
    
    prompt = f"""分析以下音乐信息，推断其音乐特征标签。

歌曲信息：
- 歌名：{title}
- 歌手：{artist}
- 专辑：{album}
- 原有流派标签：{genre or '未知'}
- 时长：{duration_sec}秒

请根据歌名、歌手、专辑名称，推断以下标签（仅输出 JSON，不要其他解释）：

{{
  "genre": "主要流派（从以下选择：pop, rock, jazz, classical, electronic, folk, rnb, hip_hop, country, ambient, blues, metal, punk, reggae, latin, indie, soul, funk, disco, lofi, ballad, acoustic, soundtrack, world, other）",
  "bpm": 节拍数（整数，60-200）,
  "energy": 能量值（0.0-1.0，快歌高、慢歌低）,
  "valence": 情绪正向度（0.0-1.0，悲伤低、快乐高）,
  "mood_tags": ["情绪标签1", "情绪标签2"]（从以下选择：happy, sad, energetic, calm, romantic, melancholy, peaceful, excited, nostalgic, hopeful, dark, bright, dreamy, aggressive, relaxing, uplifting, sentimental, playful）,
  "scene_tags": ["场景标签1", "场景标签2"]（从以下选择：morning_commute, night_drive, road_trip, party, romantic_date, focus_work, relax, workout, sleep, meditation, rainy_day, sunny_day, winter, summer, family_trip）
}}

注意：
1. 根据歌手和歌名推断，比如周杰伦通常是 pop/ballad，Linkin Park 是 rock/metal
2. 中文歌曲多为 pop/ballad，快歌 energy 高，慢歌 energy 低
3. 只输出 JSON，不要其他文字"""

    try:
        headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json"
        }
        
        data = {
            "model": "qwen-plus",
            "messages": [
                {"role": "user", "content": prompt}
            ],
            "temperature": 0.3,
            "max_tokens": 500
        }
        
        response = requests.post(
            f"{api_base}/chat/completions",
            headers=headers,
            json=data,
            timeout=30
        )
        
        if response.status_code == 200:
            result = response.json()
            content = result['choices'][0]['message']['content']
            
            json_match = content.replace('```json', '').replace('```', '').strip()
            tags = json.loads(json_match)
            
            return {
                'genre': tags.get('genre', 'other'),
                'bpm': tags.get('bpm'),
                'energy': tags.get('energy'),
                'valence': tags.get('valence'),
                'mood_tags': json.dumps(tags.get('mood_tags', []), ensure_ascii=False),
                'scene_tags': json.dumps(tags.get('scene_tags', []), ensure_ascii=False)
            }
        else:
            print(f"  LLM API 错误: {response.status_code}")
            return None
            
    except Exception as e:
        print(f"  LLM 分析失败: {e}")
        return None


def scan_music_files(input_dir: Path) -> List[Path]:
    music_files = []
    for root, _, files in os.walk(input_dir):
        for file in files:
            file_path = Path(root) / file
            if file_path.suffix.lower() in SUPPORTED_FORMATS:
                music_files.append(file_path)
    return music_files


def generate_unique_id() -> str:
    return str(uuid.uuid4())


def main():
    parser = argparse.ArgumentParser(
        description='PC端音乐元数据提取工具（生成 JSON 格式）'
    )
    
    parser.add_argument('-i', '--input', required=True, help='输入音乐目录路径')
    parser.add_argument('-o', '--output', required=True, help='输出 JSON 文件路径')
    parser.add_argument('--base', action='store_true', help='生成基础索引（不使用 LLM 分析，llm_analyzed: 0）')
    parser.add_argument('--llm', action='store_true', help='使用 LLM 分析音乐标签')
    parser.add_argument('--api-key', help='阿里云百炼 API Key（或设置 DASHSCOPE_API_KEY 环境变量）')
    parser.add_argument('--api-base', default='https://dashscope.aliyuncs.com/compatible-mode/v1', help='API 地址')
    
    args = parser.parse_args()
    
    input_dir = Path(args.input).resolve()
    output_path = Path(args.output).resolve()
    
    if not input_dir.exists():
        print(f"错误: 输入目录不存在: {input_dir}")
        sys.exit(1)
    
    output_path.parent.mkdir(parents=True, exist_ok=True)
    
    api_key = args.api_key or os.environ.get('DASHSCOPE_API_KEY')
    if args.llm and not api_key:
        print("错误: 使用 LLM 分析需要提供 API Key")
        sys.exit(1)
    
    if args.base:
        print(f"扫描目录: {input_dir}")
        music_files = scan_music_files(input_dir)
        print(f"发现 {len(music_files)} 个音乐文件")
        
        if not music_files:
            print("未找到支持的音乐文件 (MP3/FLAC/WAV/M4A/OGG)")
            sys.exit(0)
        
        tracks = []
        
        for i, file_path in enumerate(music_files, 1):
            print(f"处理 [{i}/{len(music_files)}]: {file_path.name}")
            
            metadata = get_audio_metadata(file_path)
            if not metadata:
                continue
            
            track = {
                'id': generate_unique_id(),
                'title': metadata['title'],
                'title_pinyin': metadata['title_pinyin'],
                'artist': metadata['artist'],
                'artist_pinyin': metadata['artist_pinyin'],
                'album': metadata['album'],
                'genre': metadata.get('genre'),
                'year': metadata.get('year'),
                'bpm': None,
                'energy': None,
                'valence': None,
                'mood_tags': None,
                'scene_tags': None,
                'duration_ms': metadata['duration_ms'],
                'file_path': metadata['file_path'],
                'file_size': metadata['file_size'],
                'format': metadata['format'],
                'bitrate': metadata['bitrate'],
                'sample_rate': metadata['sample_rate'],
                'llm_analyzed': 0
            }
            tracks.append(track)
        
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(tracks, f, ensure_ascii=False, indent=2)
        
        print(f"\n基础索引生成完成:")
        print(f"  成功: {len(tracks)}")
        print(f"  输出文件: {output_path}")
        return
    
    print(f"扫描目录: {input_dir}")
    music_files = scan_music_files(input_dir)
    print(f"发现 {len(music_files)} 个音乐文件")
    
    if not music_files:
        print("未找到支持的音乐文件 (MP3/FLAC/WAV/M4A/OGG)")
        sys.exit(0)
    
    tracks = []
    llm_count = 0
    
    for i, file_path in enumerate(music_files, 1):
        print(f"处理 [{i}/{len(music_files)}]: {file_path.name}")
        
        metadata = get_audio_metadata(file_path)
        if not metadata:
            continue
        
        llm_tags = None
        if args.llm and api_key:
            print(f"  LLM 分析中...")
            llm_tags = analyze_with_llm(metadata, api_key, args.api_base)
            if llm_tags:
                llm_count += 1
                print(f"  ✓ 流派: {llm_tags.get('genre')}, 能量: {llm_tags.get('energy')}, 情绪: {llm_tags.get('mood_tags')}")
            else:
                print(f"  ✗ LLM 分析失败，使用原始元数据")
            
            if i % 10 == 0:
                time.sleep(1)
        
        track = {
            'id': generate_unique_id(),
            'title': metadata['title'],
            'title_pinyin': metadata['title_pinyin'],
            'artist': metadata['artist'],
            'artist_pinyin': metadata['artist_pinyin'],
            'album': metadata['album'],
            'genre': llm_tags.get('genre') if llm_tags else metadata.get('genre'),
            'year': metadata.get('year'),
            'bpm': llm_tags.get('bpm') if llm_tags else None,
            'energy': llm_tags.get('energy') if llm_tags else None,
            'valence': llm_tags.get('valence') if llm_tags else None,
            'mood_tags': llm_tags.get('mood_tags') if llm_tags else None,
            'scene_tags': llm_tags.get('scene_tags') if llm_tags else None,
            'duration_ms': metadata['duration_ms'],
            'file_path': metadata['file_path'],
            'file_size': metadata['file_size'],
            'format': metadata['format'],
            'bitrate': metadata['bitrate'],
            'sample_rate': metadata['sample_rate'],
            'llm_analyzed': 1 if llm_tags else 0
        }
        tracks.append(track)
    
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(tracks, f, ensure_ascii=False, indent=2)
    
    print(f"\n处理完成:")
    print(f"  成功: {len(tracks)}")
    print(f"  LLM 分析: {llm_count}")
    print(f"  输出文件: {output_path}")


if __name__ == '__main__':
    main()
