<template>
  <div class="app min-h-screen bg-gradient-to-br from-gray-900 via-purple-900 to-gray-900 pb-24">
    <header class="sticky top-0 z-40 bg-gray-900/80 backdrop-blur-sm border-b border-gray-800">
      <div class="max-w-6xl mx-auto px-4 py-4 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <button
            v-if="currentView !== 'scenes'"
            @click="goBack"
            class="text-gray-400 hover:text-white transition"
          >
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <h1 class="text-xl font-bold text-white">
            {{ currentView === 'scenes' ? '🎵 场景音乐' : selectedScene?.name }}
          </h1>
        </div>
        <div v-if="currentView === 'songs'" class="text-sm text-gray-400">
          {{ selectedScene?.description }}
        </div>
      </div>
    </header>

    <main class="max-w-6xl mx-auto px-4 py-6">
      <div v-if="currentView === 'scenes'">
        <SceneSelector @select="handleSceneSelect" />
      </div>

      <div v-else-if="currentView === 'songs'">
        <div v-if="isLoading" class="flex flex-col items-center justify-center py-20">
          <div class="w-12 h-12 border-4 border-purple-500 border-t-transparent rounded-full animate-spin mb-4"></div>
          <p class="text-gray-400">正在加载推荐歌曲...</p>
        </div>
        <div v-else-if="error" class="text-center py-20">
          <p class="text-red-400 mb-4">{{ error }}</p>
          <button
            @click="retryLoad"
            class="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition"
          >
            重试
          </button>
        </div>
        <SongList
          v-else
          :songs="songs"
          :title="`${selectedScene?.icon} ${selectedScene?.name}`"
          :current-song-id="currentSong?.id"
          @play="handlePlay"
        />
      </div>
    </main>

    <Player
      :current-song="currentSong"
      :is-playing="isPlaying"
      :is-loading="isLoadingSong"
      :volume="volume"
      :progress="progress"
      :duration="duration"
      :current-time-formatted="currentTimeFormatted"
      :duration-formatted="durationFormatted"
      @toggle="togglePlay"
      @prev="playPrev"
      @next="playNext"
      @seek="seek"
      @volume="setVolume"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import SceneSelector from './components/SceneSelector.vue'
import SongList from './components/SongList.vue'
import Player from './components/Player.vue'
import { usePlayer } from './hooks/usePlayer'
import { getDiverseRecommendations } from './services/recommendEngine'

const currentView = ref('scenes')
const selectedScene = ref(null)
const songs = ref([])
const isLoading = ref(false)
const error = ref(null)

const {
  currentSong,
  isPlaying,
  isLoading: isLoadingSong,
  volume,
  progress,
  duration,
  currentTimeFormatted,
  durationFormatted,
  play,
  togglePlay,
  playPrev,
  playNext,
  seek,
  setVolume,
  setPlaylist
} = usePlayer()

const handleSceneSelect = async (scene) => {
  selectedScene.value = scene
  currentView.value = 'songs'
  await loadSongs()
}

const loadSongs = async () => {
  if (!selectedScene.value) return
  
  isLoading.value = true
  error.value = null
  
  try {
    const results = await getDiverseRecommendations(selectedScene.value.id, 3, 5)
    songs.value = results
    setPlaylist(results)
  } catch (err) {
    error.value = err.message || '加载失败，请重试'
  } finally {
    isLoading.value = false
  }
}

const retryLoad = () => {
  loadSongs()
}

const handlePlay = ({ song, index }) => {
  play(song, index)
}

const goBack = () => {
  currentView.value = 'scenes'
}
</script>

<style>
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>
