<template>
  <div 
    class="song-card flex items-center gap-4 p-3 rounded-lg cursor-pointer transition-all"
    :class="{ 'bg-purple-600/20 border border-purple-500/50': isPlaying }"
    @click="$emit('play', song)"
  >
    <div class="relative flex-shrink-0">
      <img 
        :src="song.cover || '/default-cover.png'" 
        :alt="song.name"
        class="w-14 h-14 rounded-lg object-cover"
        @error="handleImageError"
      />
      <div v-if="isPlaying" class="absolute inset-0 bg-black/50 rounded-lg flex items-center justify-center">
        <div class="playing-indicator">
          <span></span><span></span><span></span>
        </div>
      </div>
    </div>
    <div class="flex-1 min-w-0">
      <h4 class="text-white font-medium truncate">{{ song.name }}</h4>
      <p class="text-gray-400 text-sm truncate">{{ song.artist }}</p>
    </div>
    <div v-if="isPlaying" class="text-purple-400">
      <svg class="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
        <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zM7 8a1 1 0 012 0v4a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v4a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
      </svg>
    </div>
  </div>
</template>

<script setup>
defineProps({
  song: {
    type: Object,
    required: true
  },
  isPlaying: {
    type: Boolean,
    default: false
  }
})

defineEmits(['play'])

const handleImageError = (e) => {
  e.target.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="56" height="56" viewBox="0 0 56 56"%3E%3Crect fill="%23374151" width="56" height="56"/%3E%3Ctext x="28" y="32" text-anchor="middle" fill="%239CA3AF" font-size="20"%3E♪%3C/text%3E%3C/svg%3E'
}
</script>

<style scoped>
.playing-indicator {
  display: flex;
  gap: 2px;
  align-items: flex-end;
  height: 16px;
}
.playing-indicator span {
  width: 3px;
  background: #a855f7;
  animation: playing 0.6s ease-in-out infinite;
}
.playing-indicator span:nth-child(1) { animation-delay: 0s; height: 8px; }
.playing-indicator span:nth-child(2) { animation-delay: 0.2s; height: 16px; }
.playing-indicator span:nth-child(3) { animation-delay: 0.4s; height: 12px; }
@keyframes playing {
  0%, 100% { transform: scaleY(1); }
  50% { transform: scaleY(0.5); }
}
</style>
