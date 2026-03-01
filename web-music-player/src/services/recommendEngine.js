import { searchSongs } from './neteaseApi'

const sceneKeywords = {
  'late-night': ['夜曲 周杰伦', '安静 周杰伦', '深夜', '独处', '晚安'],
  'morning': ['清晨', '阳光', '早安', '活力', '唤醒'],
  'focus': ['轻音乐', '纯音乐', '专注', '工作', '学习'],
  'workout': ['运动', '动感', '节奏', '健身', '跑步'],
  'romantic': ['情歌', '浪漫', '爱情', '甜蜜', '告白'],
  'party': ['派对', '欢快', '热门', '流行', '舞曲']
}

function getRandomItem(array) {
  return array[Math.floor(Math.random() * array.length)]
}

function shuffleArray(array) {
  const shuffled = [...array]
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]]
  }
  return shuffled
}

export async function getRecommendations(sceneId, limit = 10) {
  const keywords = sceneKeywords[sceneId]

  if (!keywords || keywords.length === 0) {
    throw new Error(`未找到场景 "${sceneId}" 的关键词配置`)
  }

  const shuffledKeywords = shuffleArray(keywords)

  for (const keyword of shuffledKeywords) {
    try {
      console.log(`[推荐引擎] 使用关键词 "${keyword}" 搜索场景 "${sceneId}" 的推荐歌曲`)
      const songs = await searchSongs(keyword, limit)

      if (songs && songs.length > 0) {
        console.log(`[推荐引擎] 找到 ${songs.length} 首歌曲`)
        return songs
      }
    } catch (error) {
      console.warn(`[推荐引擎] 关键词 "${keyword}" 搜索失败:`, error.message)
    }
  }

  throw new Error(`场景 "${sceneId}" 的所有关键词搜索均失败`)
}

export async function getDiverseRecommendations(sceneId, count = 3, limitPerKeyword = 4) {
  const keywords = sceneKeywords[sceneId]

  if (!keywords || keywords.length === 0) {
    throw new Error(`未找到场景 "${sceneId}" 的关键词配置`)
  }

  const selectedKeywords = shuffleArray(keywords).slice(0, Math.min(count, keywords.length))

  const allSongs = []
  const seenIds = new Set()

  for (const keyword of selectedKeywords) {
    try {
      console.log(`[推荐引擎] 使用关键词 "${keyword}" 搜索场景 "${sceneId}" 的推荐歌曲`)
      const songs = await searchSongs(keyword, limitPerKeyword)

      for (const song of songs) {
        if (!seenIds.has(song.id)) {
          seenIds.add(song.id)
          allSongs.push(song)
        }
      }
    } catch (error) {
      console.warn(`[推荐引擎] 关键词 "${keyword}" 搜索失败:`, error.message)
    }
  }

  if (allSongs.length === 0) {
    throw new Error(`场景 "${sceneId}" 的所有关键词搜索均失败`)
  }

  console.log(`[推荐引擎] 共找到 ${allSongs.length} 首不重复歌曲`)
  return shuffleArray(allSongs)
}

export function getSceneKeywords(sceneId) {
  return sceneKeywords[sceneId] || []
}

export function getAllScenes() {
  return Object.keys(sceneKeywords)
}

export default {
  getRecommendations,
  getDiverseRecommendations,
  getSceneKeywords,
  getAllScenes
}
