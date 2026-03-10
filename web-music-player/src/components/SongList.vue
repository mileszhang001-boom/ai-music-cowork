<template>
  <div class="song-list">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-xl font-bold text-white">{{ title }}</h2>
      <span class="text-gray-400 text-sm">{{ songs.length }} 首歌曲</span>
    </div>
    <div class="songs-container space-y-2 max-h-[calc(100vh-280px)] overflow-y-auto pr-2">
      <SongCard
        v-for="(song, index) in songs"
        :key="song.id"
        :song="song"
        :is-playing="currentSongId === song.id"
        @play="handlePlay(song, index)"
      />
    </div>
  </div>
</template>

<script setup>
import SongCard from './SongCard.vue'

defineProps({
  songs: {
    type: Array,
    default: () => []
  },
  title: {
    type: String,
    default: '歌曲列表'
  },
  currentSongId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['play'])

const handlePlay = (song, index) => {
  emit('play', { song, index })
}
</script>

<style scoped>
.songs-container::-webkit-scrollbar {
  width: 6px;
}
.songs-container::-webkit-scrollbar-track {
  background: #1f2937;
  border-radius: 3px;
}
.songs-container::-webkit-scrollbar-thumb {
  background: #4b5563;
  border-radius: 3px;
}
.songs-container::-webkit-scrollbar-thumb:hover {
  background: #6b7280;
}
</style>
