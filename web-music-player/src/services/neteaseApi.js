import axios from 'axios'

const BASE_URL = 'http://localhost:3000'

const api = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
})

export async function searchSongs(keywords, limit = 10) {
  try {
    const response = await api.get('/search', {
      params: {
        keywords,
        limit,
      },
    })

    if (response.data.code !== 200) {
      throw new Error(response.data.message || '搜索失败')
    }

    const songs = response.data.result.songs || []

    return songs.map((song) => ({
      id: song.id,
      name: song.name,
      artist: song.artists?.map((a) => a.name).join(', ') || '',
      album: song.album?.name || '',
      cover: song.album?.artist?.img1v1Url || song.album?.blurPicUrl || '',
    }))
  } catch (error) {
    console.error('搜索歌曲失败:', error.message)
    throw error
  }
}

export async function getSongDetail(id) {
  try {
    const response = await api.get('/song/detail', {
      params: {
        ids: id,
      },
    })

    if (response.data.code !== 200) {
      throw new Error(response.data.message || '获取歌曲详情失败')
    }

    const song = response.data.songs?.[0]

    if (!song) {
      throw new Error('歌曲不存在')
    }

    return {
      id: song.id,
      name: song.name,
      artist: song.ar?.map((a) => a.name).join(', ') || '',
      artistId: song.ar?.[0]?.id,
      album: song.al?.name || '',
      albumId: song.al?.id,
      cover: song.al?.picUrl || '',
      duration: song.dt,
      publishTime: song.publishTime,
    }
  } catch (error) {
    console.error('获取歌曲详情失败:', error.message)
    throw error
  }
}

export async function getSongUrl(id) {
  try {
    const response = await api.get('/song/url', {
      params: {
        id,
      },
    })

    if (response.data.code !== 200) {
      throw new Error(response.data.message || '获取播放URL失败')
    }

    const data = response.data.data?.[0]

    if (!data || !data.url) {
      throw new Error('无法获取播放链接')
    }

    return {
      url: data.url,
      size: data.size,
      type: data.type,
      br: data.br,
    }
  } catch (error) {
    console.error('获取播放URL失败:', error.message)
    throw error
  }
}

export async function getSongDetailWithUrl(id) {
  try {
    const [detail, urlInfo] = await Promise.all([getSongDetail(id), getSongUrl(id)])

    return {
      ...detail,
      ...urlInfo,
    }
  } catch (error) {
    console.error('获取歌曲完整信息失败:', error.message)
    throw error
  }
}

export function setBaseUrl(url) {
  api.defaults.baseURL = url
}

export default {
  searchSongs,
  getSongDetail,
  getSongUrl,
  getSongDetailWithUrl,
  setBaseUrl,
}
