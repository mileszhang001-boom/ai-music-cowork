#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
合并音乐索引文件
将现有的已分析索引与新扫描的文件路径合并
"""

import json
import os
from pathlib import Path

def load_json(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        return json.load(f)

def save_json(filepath, data):
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

def extract_title_artist(filename):
    """从文件名提取标题和艺术家"""
    name = Path(filename).stem
    if ' - ' in name:
        parts = name.split(' - ', 1)
        return parts[1].strip(), parts[0].strip()
    return name, None

def normalize_title(title):
    """标准化标题用于匹配"""
    return title.lower().strip()

def main():
    old_index_path = '/Users/bella/ai-music-cowork/data/index.json'
    new_index_path = '/Users/bella/ai-music-cowork/data/index_new.json'
    output_path = '/Users/bella/ai-music-cowork/data/index_merged.json'
    
    print("加载现有索引...")
    old_tracks = load_json(old_index_path)
    print(f"现有索引: {len(old_tracks)} 首歌曲")
    
    print("\n加载新扫描索引...")
    new_tracks = load_json(new_index_path)
    print(f"新扫描索引: {len(new_tracks)} 首歌曲")
    
    # 创建旧索引的映射表（标题 -> 完整数据）
    old_map = {}
    for track in old_tracks:
        title = normalize_title(track.get('title', ''))
        if title:
            old_map[title] = track
    
    print(f"\n开始合并...")
    
    merged_tracks = []
    matched_count = 0
    new_count = 0
    
    for new_track in new_tracks:
        title = normalize_title(new_track.get('title', ''))
        
        # 尝试匹配旧索引
        if title in old_map:
            old_track = old_map[title]
            # 合并数据：使用旧索引的标签，新索引的文件信息
            merged_track = {
                **new_track,  # 新扫描的基础信息（文件路径、大小等）
                # 使用旧索引的分析数据
                'genre': old_track.get('genre'),
                'bpm': old_track.get('bpm'),
                'energy': old_track.get('energy'),
                'valence': old_track.get('valence'),
                'mood_tags': old_track.get('mood_tags'),
                'scene_tags': old_track.get('scene_tags'),
                'llm_analyzed': old_track.get('llm_analyzed', 1)
            }
            merged_tracks.append(merged_track)
            matched_count += 1
        else:
            # 新歌曲，保留基本信息
            merged_tracks.append(new_track)
            new_count += 1
    
    print(f"\n合并结果:")
    print(f"  匹配成功: {matched_count} 首（使用已有标签）")
    print(f"  新增歌曲: {new_count} 首（需要后续分析）")
    print(f"  总计: {len(merged_tracks)} 首")
    
    # 按ID排序
    merged_tracks.sort(key=lambda x: x.get('id', 0))
    
    # 重新分配ID
    for i, track in enumerate(merged_tracks, 1):
        track['id'] = i
    
    print(f"\n保存合并后的索引到: {output_path}")
    save_json(output_path, merged_tracks)
    
    # 统计信息
    analyzed_count = sum(1 for t in merged_tracks if t.get('llm_analyzed'))
    print(f"\n统计:")
    print(f"  已分析: {analyzed_count} 首")
    print(f"  未分析: {len(merged_tracks) - analyzed_count} 首")
    
    # 显示示例
    print("\n示例（前3首已分析）:")
    analyzed = [t for t in merged_tracks if t.get('llm_analyzed')][:3]
    for track in analyzed:
        print(f"  - {track['title']} ({track.get('genre', 'N/A')}, energy: {track.get('energy', 'N/A')})")
    
    print("\n示例（前3首未分析）:")
    unanalyzed = [t for t in merged_tracks if not t.get('llm_analyzed')][:3]
    for track in unanalyzed:
        print(f"  - {track['title']}")

if __name__ == '__main__':
    main()
