#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PC端音乐元数据提取工具
用于提取音乐文件的元数据并生成 SQLite 索引数据库
"""

import argparse
import os
import sqlite3
import sys
from pathlib import Path
from typing import Optional, Dict, Any, List

try:
    from mutagen import File as MutagenFile
    from mutagen.flac import FLAC
    from mutagen.mp3 import MP3
    from mutagen.wavpack import WavPack
    from mutagen.m4a import M4A
    from mutagen.id3 import ID3NoHeaderError
except ImportError:
    print("错误: 请先安装 mutagen 库: pip install mutagen")
    sys.exit(1)

try:
    from pypinyin import lazy_pinyin, Style
    PINYIN_AVAILABLE = True
except ImportError:
    PINYIN_AVAILABLE = False
    print("警告: pypinyin 库未安装，中文拼音转换功能将不可用")


SUPPORTED_FORMATS = {'.mp3', '.flac', '.wav', '.m4a'}


def to_pinyin(text: str) -> Optional[str]:
    """将中文转换为拼音"""
    if not text or not PINYIN_AVAILABLE:
        return None
    
    try:
        pinyin_list = lazy_pinyin(text, style=Style.NORMAL)
        return ' '.join(pinyin_list)
    except Exception:
        return None


def get_audio_metadata(file_path: Path) -> Optional[Dict[str, Any]]:
    """提取音频文件的元数据"""
    try:
        audio = MutagenFile(str(file_path), easy=True)
        if audio is None:
            return None
        
        metadata = {
            'title': None,
            'artist': None,
            'album': None,
            'genre': None,
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
        elif file_path.suffix.lower() == '.mp3':
            audio_full = MP3(str(file_path))
            if audio_full.tags:
                metadata['title'] = str(audio_full.tags.get('TIT2', [''])[0]) if audio_full.tags.get('TIT2') else None
                metadata['artist'] = str(audio_full.tags.get('TPE1', [''])[0]) if audio_full.tags.get('TPE1') else None
                metadata['album'] = str(audio_full.tags.get('TALB', [''])[0]) if audio_full.tags.get('TALB') else None
                metadata['genre'] = str(audio_full.tags.get('TCON', [''])[0]) if audio_full.tags.get('TCON') else None
        elif file_path.suffix.lower() == '.m4a':
            audio_full = M4A(str(file_path))
            metadata['title'] = audio_full.get('\xa9nam', [None])[0]
            metadata['artist'] = audio_full.get('\xa9ART', [None])[0]
            metadata['album'] = audio_full.get('\xa9alb', [None])[0]
            metadata['genre'] = audio_full.get('\xa9gen', [None])[0]
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


def scan_music_files(input_dir: Path) -> List[Path]:
    """扫描目录下的所有音乐文件"""
    music_files = []
    
    for root, _, files in os.walk(input_dir):
        for file in files:
            file_path = Path(root) / file
            if file_path.suffix.lower() in SUPPORTED_FORMATS:
                music_files.append(file_path)
    
    return music_files


def create_database(db_path: Path) -> sqlite3.Connection:
    """创建 SQLite 数据库和表结构"""
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
            play_count INTEGER DEFAULT 0
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
    
    conn.commit()
    return conn


def insert_track(conn: sqlite3.Connection, metadata: Dict[str, Any]) -> int:
    """插入音轨数据到数据库"""
    cursor = conn.cursor()
    
    cursor.execute('''
        INSERT INTO tracks (
            title, title_pinyin, artist, artist_pinyin, album, genre,
            duration_ms, file_path, file_size, format, bitrate, sample_rate
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ''', (
        metadata['title'],
        metadata['title_pinyin'],
        metadata['artist'],
        metadata['artist_pinyin'],
        metadata['album'],
        metadata['genre'],
        metadata['duration_ms'],
        metadata['file_path'],
        metadata['file_size'],
        metadata['format'],
        metadata['bitrate'],
        metadata['sample_rate']
    ))
    
    conn.commit()
    return cursor.lastrowid


def export_to_json(conn: sqlite3.Connection, output_path: Path):
    """导出数据库为 JSON 格式"""
    import json
    
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


def export_to_csv(conn: sqlite3.Connection, output_path: Path):
    """导出数据库为 CSV 格式"""
    import csv
    
    cursor = conn.cursor()
    cursor.execute('SELECT * FROM tracks')
    columns = [description[0] for description in cursor.description]
    
    with open(output_path, 'w', encoding='utf-8', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=columns)
        writer.writeheader()
        
        for row in cursor.fetchall():
            track = dict(zip(columns, row))
            writer.writerow(track)
    
    print(f"已导出到 {output_path}")


def main():
    parser = argparse.ArgumentParser(
        description='PC端音乐元数据提取工具',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog='''
示例:
  %(prog)s --input /path/to/music --output /path/to/index.db
  %(prog)s -i ~/Music -o ./music_index.db --format json
  %(prog)s -i /music -o ./index.db --format csv
        '''
    )
    
    parser.add_argument(
        '-i', '--input',
        required=True,
        help='输入音乐目录路径'
    )
    
    parser.add_argument(
        '-o', '--output',
        required=True,
        help='输出数据库文件路径'
    )
    
    parser.add_argument(
        '-f', '--format',
        choices=['db', 'json', 'csv', 'all'],
        default='db',
        help='输出格式: db(SQLite), json, csv, all (默认: db)'
    )
    
    args = parser.parse_args()
    
    input_dir = Path(args.input).resolve()
    output_path = Path(args.output).resolve()
    
    if not input_dir.exists():
        print(f"错误: 输入目录不存在: {input_dir}")
        sys.exit(1)
    
    if not input_dir.is_dir():
        print(f"错误: 输入路径不是目录: {input_dir}")
        sys.exit(1)
    
    output_path.parent.mkdir(parents=True, exist_ok=True)
    
    print(f"扫描目录: {input_dir}")
    music_files = scan_music_files(input_dir)
    print(f"发现 {len(music_files)} 个音乐文件")
    
    if not music_files:
        print("未找到支持的音乐文件 (MP3/FLAC/WAV/M4A)")
        sys.exit(0)
    
    db_path = output_path if output_path.suffix == '.db' else output_path.with_suffix('.db')
    conn = create_database(db_path)
    
    success_count = 0
    error_count = 0
    
    for i, file_path in enumerate(music_files, 1):
        print(f"处理 [{i}/{len(music_files)}]: {file_path.name}")
        
        metadata = get_audio_metadata(file_path)
        if metadata:
            insert_track(conn, metadata)
            success_count += 1
        else:
            error_count += 1
    
    conn.close()
    
    print(f"\n处理完成:")
    print(f"  成功: {success_count}")
    print(f"  失败: {error_count}")
    print(f"  数据库: {db_path}")
    
    if args.format in ['json', 'all']:
        json_path = db_path.with_suffix('.json')
        conn = sqlite3.connect(str(db_path))
        export_to_json(conn, json_path)
        conn.close()
    
    if args.format in ['csv', 'all']:
        csv_path = db_path.with_suffix('.csv')
        conn = sqlite3.connect(str(db_path))
        export_to_csv(conn, csv_path)
        conn.close()


if __name__ == '__main__':
    main()
