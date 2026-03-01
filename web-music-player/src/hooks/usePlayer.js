import { ref, computed, onUnmounted } from 'vue'
import { Howl } from 'howler'
import { getSongUrl } from '../services/neteaseApi'

export function usePlayer() {
  const currentSong = ref(null)
  const currentSongIndex = ref(-1)
  const playlist = ref([])
  const isPlaying = ref(false)
  const isLoading = ref(false)
  const volume = ref(0.7)
  const progress = ref(0)
  const duration = ref(0)
  const error = ref(null)

  let howl = null
  let progressInterval = null

  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  const currentTimeFormatted = computed(() => formatTime(progress.value))
  const durationFormatted = computed(() => formatTime(duration.value))

  const clearProgressInterval = () => {
    if (progressInterval) {
      clearInterval(progressInterval)
      progressInterval = null
    }
  }

  const startProgressTracking = () => {
    clearProgressInterval()
    progressInterval = setInterval(() => {
      if (howl && isPlaying.value) {
        progress.value = howl.seek()
      }
    }, 100)
  }

  const loadSong = async (song) => {
    try {
      isLoading.value = true
      error.value = null

      const urlInfo = await getSongUrl(song.id)

      if (!urlInfo.url) {
        throw new Error('无法获取播放链接')
      }

      if (howl) {
        howl.unload()
      }

      howl = new Howl({
        src: [urlInfo.url],
        html5: true,
        volume: volume.value,
        onload: () => {
          duration.value = howl.duration()
          isLoading.value = false
        },
        onplay: () => {
          isPlaying.value = true
          startProgressTracking()
        },
        onpause: () => {
          isPlaying.value = false
          clearProgressInterval()
        },
        onstop: () => {
          isPlaying.value = false
          clearProgressInterval()
        },
        onend: () => {
          isPlaying.value = false
          clearProgressInterval()
          playNext()
        },
        onloaderror: (id, err) => {
          error.value = '加载失败'
          isLoading.value = false
          console.error('播放错误:', err)
        }
      })

      currentSong.value = song
      return true
    } catch (err) {
      error.value = err.message
      isLoading.value = false
      return false
    }
  }

  const play = async (song, index) => {
    if (currentSong.value?.id === song.id && howl) {
      if (isPlaying.value) {
        howl.pause()
      } else {
        howl.play()
      }
      return
    }

    currentSongIndex.value = index
    const success = await loadSong(song)
    if (success) {
      howl.play()
    }
  }

  const pause = () => {
    if (howl && isPlaying.value) {
      howl.pause()
    }
  }

  const resume = () => {
    if (howl && !isPlaying.value) {
      howl.play()
    }
  }

  const togglePlay = () => {
    if (isPlaying.value) {
      pause()
    } else {
      resume()
    }
  }

  const playNext = () => {
    if (playlist.value.length === 0) return
    const nextIndex = (currentSongIndex.value + 1) % playlist.value.length
    play(playlist.value[nextIndex], nextIndex)
  }

  const playPrev = () => {
    if (playlist.value.length === 0) return
    const prevIndex = currentSongIndex.value <= 0
      ? playlist.value.length - 1
      : currentSongIndex.value - 1
    play(playlist.value[prevIndex], prevIndex)
  }

  const seek = (time) => {
    if (howl) {
      howl.seek(time)
      progress.value = time
    }
  }

  const setVolume = (val) => {
    volume.value = val
    if (howl) {
      howl.volume(val)
    }
  }

  const setPlaylist = (songs) => {
    playlist.value = songs
  }

  const stop = () => {
    if (howl) {
      howl.stop()
      howl.unload()
      howl = null
    }
    currentSong.value = null
    currentSongIndex.value = -1
    isPlaying.value = false
    progress.value = 0
    duration.value = 0
    clearProgressInterval()
  }

  onUnmounted(() => {
    stop()
  })

  return {
    currentSong,
    currentSongIndex,
    playlist,
    isPlaying,
    isLoading,
    volume,
    progress,
    duration,
    error,
    currentTimeFormatted,
    durationFormatted,
    play,
    pause,
    resume,
    togglePlay,
    playNext,
    playPrev,
    seek,
    setVolume,
    setPlaylist,
    stop
  }
}
