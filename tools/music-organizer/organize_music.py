#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PC端音乐元数据提取工具
用于提取音乐文件的元数据并生成 SQLite 索引数据库
支持 LLM 分析音乐标签
"""

import argparse
import json
import os
import sqlite3
import sys
import time
from pathlib import Path
from typing import Optional, Dict, Any, List

try:
    from mutagen import File as MutagenFile
    from mutagen.flac import FLAC
    from mutagen.mp3 import MP3
    from mutagen.m4a import M4A
except ImportError:
    print("错误: 请先安装 mutagen 库: pip install mutagen")
    sys.exit(1)

try:
    from pypinyin import lazy_pinyin, Style
    PINYIN_AVAILABLE = True
except ImportError:
    PINYIN_AVAILABLE = False
    print("警告: pypinyin 库未安装，中文拼音转换功能将不可用")

try:
    import requests
    REQUESTS_AVAILABLE = True
except ImportError:
    REQUESTS_AVAILABLE = False

SUPPORTED_FORMATS = {'.mp3', '.flac', '.wav', '.m4a'}

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
        else:
            metadata['title'] = audio.get('title', [None])[0]
            metadata['artist'] = audio.get('artist', [None])[0]
            metadata['album'] = audio.get('album', [None])[0]
            metadata['genre'] = audio.get('genre', [None])[0]
        
        if not metadata['title']:
            metadata['title'] = file_path.stem
        
        if not metadata['artist']:
            metadata['artist'] = '未知艺术家'
        
        metadata['title_pinyin'] = to_pinyin(metadata['title'])
        metadata['artist_pinyin'] = to_pinyin(metadata['artist'])
        
        return metadata
    
    except Exception as e:
        print(f"警告: 无法读取文件 {file_path}: {e}")
        return None


def analyze_with_llm(metadata: Dict[str, Any], api_key: str, api_base: str = "https://dashscope.aliyuncs.com/compatible-mode/v1") -> Optional[Dict[str, Any]]:
    """使用 LLM 分析音乐标签"""
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


def create_database(db_path: Path) -> sqlite3.Connection:
    conn = sqlite3.connect(str(db_path))
    cursor = conn.cursor()
    
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS tracks (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            title_pinyin TEXT,
            artist TEXT NOT NULL,
            artist_pinyin TEXT,
            album TEXT,
            genre TEXT,
            year TEXT,
            bpm INTEGER,
            energy REAL,
            valence REAL,
            mood_tags TEXT,
            scene_tags TEXT,
            duration_ms INTEGER,
            file_path TEXT NOT NULL,
            file_size INTEGER,
            format TEXT,
            bitrate INTEGER,
            sample_rate INTEGER,
            play_count INTEGER DEFAULT 0,
            llm_analyzed INTEGER DEFAULT 0
        )
    ''')
    
    cursor.execute('''
        CREATE VIRTUAL TABLE IF NOT EXISTS tracks_fts USING fts5(
            title, title_pinyin, artist, artist_pinyin,
            album, genre, mood_tags, scene_tags,
            content='tracks', content_rowid='id'
        )
    ''')
    
    cursor.execute('''
        CREATE TRIGGER IF NOT EXISTS tracks_ai AFTER INSERT ON tracks BEGIN
            INSERT INTO tracks_fts(rowid, title, title_pinyin, artist, artist_pinyin, album, genre, mood_tags, scene_tags)
            VALUES (new.id, new.title, new.title_pinyin, new.artist, new.artist_pinyin, new.album, new.genre, new.mood_tags, new.scene_tags);
        END
    ''')
    
    cursor.execute('''
        CREATE TRIGGER IF NOT EXISTS tracks_ad AFTER DELETE ON tracks BEGIN
            INSERT INTO tracks_fts(tracks_fts, rowid, title, title_pinyin, artist, artist_pinyin, album, genre, mood_tags, scene_tags)
            VALUES('delete', old.id, old.title, old.title_pinyin, old.artist, old.artist_pinyin, old.album, old.genre, old.mood_tags, old.scene_tags);
        END
    ''')
    
    cursor.execute('''
        CREATE TRIGGER IF NOT EXISTS tracks_au AFTER UPDATE ON tracks BEGIN
            INSERT INTO tracks_fts(tracks_fts, rowid, title, title_pinyin, artist, artist_pinyin, album, genre, mood_tags, scene_tags)
            VALUES('delete', old.id, old.title, old.title_pinyin, old.artist, old.artist_pinyin, old.album, old.genre, old.mood_tags, old.scene_tags);
            INSERT INTO tracks_fts(rowid, title, title_pinyin, artist, artist_pinyin, album, genre, mood_tags, scene_tags)
            VALUES (new.id, new.title, new.title_pinyin, new.artist, new.artist_pinyin, new.album, new.genre, new.mood_tags, new.scene_tags);
        END
    ''')
    
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_genre ON tracks(genre)')
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_energy ON tracks(energy)')
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_bpm ON tracks(bpm)')
    
    conn.commit()
    return conn


def insert_track(conn: sqlite3.Connection, metadata: Dict[str, Any], llm_tags: Optional[Dict[str, Any]] = None) -> int:
    cursor = conn.cursor()
    
    genre = llm_tags.get('genre') if llm_tags else metadata.get('genre')
    bpm = llm_tags.get('bpm') if llm_tags else None
    energy = llm_tags.get('energy') if llm_tags else None
    valence = llm_tags.get('valence') if llm_tags else None
    mood_tags = llm_tags.get('mood_tags') if llm_tags else None
    scene_tags = llm_tags.get('scene_tags') if llm_tags else None
    llm_analyzed = 1 if llm_tags else 0
    
    cursor.execute('''
        INSERT INTO tracks (
            title, title_pinyin, artist, artist_pinyin, album, genre, year,
            bpm, energy, valence, mood_tags, scene_tags,
            duration_ms, file_path, file_size, format, bitrate, sample_rate, llm_analyzed
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ''', (
        metadata['title'],
        metadata['title_pinyin'],
        metadata['artist'],
        metadata['artist_pinyin'],
        metadata['album'],
        genre,
        metadata.get('year'),
        bpm,
        energy,
        valence,
        mood_tags,
        scene_tags,
        metadata['duration_ms'],
        metadata['file_path'],
        metadata['file_size'],
        metadata['format'],
        metadata['bitrate'],
        metadata['sample_rate'],
        llm_analyzed
    ))
    
    conn.commit()
    return cursor.lastrowid


def export_to_json(conn: sqlite3.Connection, output_path: Path):
    cursor = conn.cursor()
    cursor.execute('SELECT * FROM tracks')
    columns = [description[0] for description in cursor.description]
    
    tracks = []
    for row in cursor.fetchall():
        track = dict(zip(columns, row))
        tracks.append(track)
    
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(tracks, f, ensure_ascii=False, indent=2)
    
    print(f"已导出 {len(tracks)} 条记录到 {output_path}")


def main():
    parser = argparse.ArgumentParser(
        description='PC端音乐元数据提取工具（支持 LLM 分析标签）',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog='''
示例:
  # 基本用法（只提取元数据）
  %(prog)s -i /path/to/music -o ./index.db
  
  # 使用 LLM 分析标签
  %(prog)s -i /path/to/music -o ./index.db --llm --api-key YOUR_API_KEY
  
  # 使用环境变量设置 API Key
  export DASHSCOPE_API_KEY=YOUR_API_KEY
  %(prog)s -i /path/to/music -o ./index.db --llm
        '''
    )
    
    parser.add_argument('-i', '--input', required=True, help='输入音乐目录路径')
    parser.add_argument('-o', '--output', required=True, help='输出数据库文件路径')
    parser.add_argument('-f', '--format', choices=['db', 'json', 'all'], default='db', help='输出格式')
    parser.add_argument('--llm', action='store_true', help='使用 LLM 分析音乐标签')
    parser.add_argument('--api-key', help='阿里云百炼 API Key（或设置 DASHSCOPE_API_KEY 环境变量）')
    parser.add_argument('--api-base', default='https://dashscope.aliyuncs.com/compatible-mode/v1', help='API 地址')
    parser.add_argument('--batch-size', type=int, default=10, help='LLM 批量处理大小')
    
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
        print("  方式1: --api-key YOUR_API_KEY")
        print("  方式2: export DASHSCOPE_API_KEY=YOUR_API_KEY")
        sys.exit(1)
    
    print(f"扫描目录: {input_dir}")
    music_files = scan_music_files(input_dir)
    print(f"发现 {len(music_files)} 个音乐文件")
    
    if not music_files:
        print("未找到支持的音乐文件 (MP3/FLAC/WAV/M4A)")
        sys.exit(0)
    
    db_path = output_path if output_path.suffix == '.db' else output_path.with_suffix('.db')
    conn = create_database(db_path)
    
    success_count = 0
    llm_count = 0
    error_count = 0
    
    for i, file_path in enumerate(music_files, 1):
        print(f"处理 [{i}/{len(music_files)}]: {file_path.name}")
        
        metadata = get_audio_metadata(file_path)
        if not metadata:
            error_count += 1
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
            
            if i % args.batch_size == 0:
                time.sleep(1)
        
        insert_track(conn, metadata, llm_tags)
        success_count += 1
    
    conn.close()
    
    print(f"\n处理完成:")
    print(f"  成功: {success_count}")
    print(f"  LLM 分析: {llm_count}")
    print(f"  失败: {error_count}")
    print(f"  数据库: {db_path}")
    
    if args.format in ['json', 'all']:
        json_path = db_path.with_suffix('.json')
        conn = sqlite3.connect(str(db_path))
        export_to_json(conn, json_path)
        conn.close()


if __name__ == '__main__':
    main()
