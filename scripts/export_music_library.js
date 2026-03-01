const fs = require('fs');
const path = require('path');

const INPUT_FILE = '/Users/bella/ai-music-cowork/data/music_library.json';
const OUTPUT_DIR = '/Users/bella/ai-music-cowork/data';

const CSV_HEADERS = [
  'ID',
  '歌曲名称',
  '歌手',
  '流派',
  'BPM',
  '能量值',
  '时长(秒)',
  '语言',
  '情感表达',
  '用户反馈',
  '场景匹配',
  '所属场景'
];

const FIELD_MAPPING = {
  'ID': 'id',
  '歌曲名称': 'title',
  '歌手': 'artist',
  '流派': 'genre',
  'BPM': 'bpm',
  '能量值': 'energy',
  '时长(秒)': 'duration',
  '语言': 'language',
  '情感表达': 'expression',
  '用户反馈': 'user_feedback',
  '场景匹配': 'scene_match',
  '所属场景': 'scene_type'
};

function escapeCSVField(field) {
  if (field === null || field === undefined) {
    return '';
  }
  const str = String(field);
  if (str.includes(',') || str.includes('"') || str.includes('\n')) {
    return '"' + str.replace(/"/g, '""') + '"';
  }
  return str;
}

function extractAllTracks(data) {
  const allTracks = [];
  const scenes = data.scenes || {};
  
  for (const [sceneKey, sceneData] of Object.entries(scenes)) {
    const tracks = sceneData.tracks || [];
    for (const track of tracks) {
      allTracks.push({
        ...track,
        scene_type: sceneKey
      });
    }
  }
  
  return allTracks;
}

function exportToCSV(tracks, outputPath) {
  const lines = [];
  
  lines.push(CSV_HEADERS.join(','));
  
  for (const track of tracks) {
    const row = CSV_HEADERS.map(header => {
      const fieldName = FIELD_MAPPING[header];
      const value = track[fieldName];
      return escapeCSVField(value);
    });
    lines.push(row.join(','));
  }
  
  fs.writeFileSync(outputPath, lines.join('\n'), 'utf-8');
  console.log(`CSV 导出成功: ${outputPath}`);
  console.log(`共导出 ${tracks.length} 首歌曲`);
}

function exportToMarkdown(tracks, outputPath) {
  const lines = [];
  
  const headerRow = '| ' + CSV_HEADERS.join(' | ') + ' |';
  lines.push(headerRow);
  
  const separatorRow = '| ' + CSV_HEADERS.map(() => '---').join(' | ') + ' |';
  lines.push(separatorRow);
  
  for (const track of tracks) {
    const row = CSV_HEADERS.map(header => {
      const fieldName = FIELD_MAPPING[header];
      const value = track[fieldName];
      if (value === null || value === undefined) {
        return '';
      }
      const str = String(value);
      return str.replace(/\|/g, '\\|').replace(/\n/g, ' ');
    });
    lines.push('| ' + row.join(' | ') + ' |');
  }
  
  fs.writeFileSync(outputPath, lines.join('\n'), 'utf-8');
  console.log(`Markdown 导出成功: ${outputPath}`);
  console.log(`共导出 ${tracks.length} 首歌曲`);
}

function main() {
  const args = process.argv.slice(2);
  const format = args[0] || 'both';
  
  if (!['csv', 'md', 'both'].includes(format)) {
    console.error('用法: node scripts/export_music_library.js [csv|md|both]');
    console.error('  csv  - 仅导出CSV格式');
    console.error('  md   - 仅导出Markdown格式');
    console.error('  both - 导出两种格式（默认）');
    process.exit(1);
  }
  
  if (!fs.existsSync(INPUT_FILE)) {
    console.error(`错误: 找不到输入文件 ${INPUT_FILE}`);
    process.exit(1);
  }
  
  console.log('正在读取音乐库数据...');
  const rawData = fs.readFileSync(INPUT_FILE, 'utf-8');
  const data = JSON.parse(rawData);
  
  console.log('正在提取歌曲信息...');
  const tracks = extractAllTracks(data);
  
  if (tracks.length === 0) {
    console.error('警告: 未找到任何歌曲数据');
    process.exit(0);
  }
  
  if (format === 'csv' || format === 'both') {
    const csvPath = path.join(OUTPUT_DIR, 'music_library_export.csv');
    exportToCSV(tracks, csvPath);
  }
  
  if (format === 'md' || format === 'both') {
    const mdPath = path.join(OUTPUT_DIR, 'music_library_export.md');
    exportToMarkdown(tracks, mdPath);
  }
  
  console.log('\n导出完成!');
}

main();
