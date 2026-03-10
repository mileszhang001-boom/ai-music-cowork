<template>
  <div class="player fixed bottom-0 left-0 right-0 bg-gray-900 border-t border-gray-800 px-4 py-3 z-50">
    <div class="max-w-6xl mx-auto flex items-center gap-4">
      <div class="flex items-center gap-3 flex-1 min-w-0">
        <img
          :src="currentSong?.cover || defaultCover"
          :alt="currentSong?.name"
          class="w-12 h-12 rounded-lg object-cover flex-shrink-0"
        />
        <div class="min-w-0">
          <h4 class="text-white font-medium truncate text-sm">{{ currentSong?.name || '未播放' }}</h4>
          <p class="text-gray-400 text-xs truncate">{{ currentSong?.artist || '--' }}</p>
        </div>
      </div>

      <div class="flex flex-col items-center gap-1 flex-1">
        <div class="flex items-center gap-4">
          <button @click="$emit('prev')" class="text-gray-400 hover:text-white transition">
            <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
              <path d="M8.445 14.832A1 1 0 0010 14v-2.798l5.445 3.63A1 1 0 0017 14V6a1 1 0 00-1.555-.832L10 8.798V6a1 1 0 00-1.555-.832l-6 4a1 1 0 000 1.664l6 4z" />
            </svg>
          </button>
          <button
            @click="$emit('toggle')"
            class="w-10 h-10 rounded-full bg-white flex items-center justify-center hover:scale-105 transition"
            :disabled="!currentSong"
          >
            <svg v-if="isLoading" class="w-5 h-5 text-gray-900 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <svg v-else-if="isPlaying" class="w-5 h-5 text-gray-900" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zM7 8a1 1 0 012 0v4a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v4a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
            </svg>
            <svg v-else class="w-5 h-5 text-gray-900 ml-0.5" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z" clip-rule="evenodd" />
            </svg>
          </button>
          <button @click="$emit('next')" class="text-gray-400 hover:text-white transition">
            <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
              <path d="M11.555 5.168A1 1 0 0010 6v2.798L4.555 5.168A1 1 0 003 6v8a1 1 0 001.555.832L10 11.202V14a1 1 0 001.555.832l6-4a1 1 0 000-1.664l-6-4z" />
            </svg>
          </button>
        </div>
        <div class="flex items-center gap-2 w-full max-w-md">
          <span class="text-xs text-gray-400 w-10 text-right">{{ currentTimeFormatted }}</span>
          <div class="flex-1 h-1 bg-gray-700 rounded-full cursor-pointer group" @click="handleSeek">
            <div class="h-full bg-purple-500 rounded-full relative" :style="{ width: progressPercent + '%' }">
              <div class="absolute right-0 top-1/2 -translate-y-1/2 w-3 h-3 bg-white rounded-full opacity-0 group-hover:opacity-100 transition"></div>
            </div>
          </div>
          <span class="text-xs text-gray-400 w-10">{{ durationFormatted }}</span>
        </div>
      </div>

      <div class="flex items-center gap-2 flex-1 justify-end">
        <button @click="toggleMute" class="text-gray-400 hover:text-white transition">
          <svg v-if="volume === 0" class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M9.383 3.076A1 1 0 0110 4v12a1 1 0 01-1.707.707L4.586 13H2a1 1 0 01-1-1V8a1 1 0 011-1h2.586l3.707-3.707a1 1 0 011.09-.217zM14.657 2.929a1 1 0 011.414 0A9.972 9.972 0 0119 10a9.972 9.972 0 01-2.929 7.071 1 1 0 01-1.414-1.414A7.971 7.971 0 0017 10c0-2.21-.894-4.208-2.343-5.657a1 1 0 010-1.414zm-2.829 2.828a1 1 0 011.415 0A5.983 5.983 0 0115 10a5.984 5.984 0 01-1.757 4.243 1 1 0 01-1.415-1.415A3.984 3.984 0 0013 10a3.983 3.983 0 00-1.172-2.828 1 1 0 010-1.415z" clip-rule="evenodd" />
          </svg>
          <svg v-else-if="volume < 0.5" class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
            <path d="M9.383 3.076A1 1 0 0110 4v12a1 1 0 01-1.707.707L4.586 13H2a1 1 0 01-1-1V8a1 1 0 011-1h2.586l3.707-3.707a1 1 0 011.09-.217zM14.657 2.929a1 1 0 011.414 0A9.972 9.972 0 0119 10a9.972 9.972 0 01-2.929 7.071 1 1 0 01-1.414-1.414A7.971 7.971 0 0017 10c0-2.21-.894-4.208-2.343-5.657a1 1 0 010-1.414z" />
          </svg>
          <svg v-else class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
            <path d="M9.383 3.076A1 1 0 0110 4v12a1 1 0 01-1.707.707L4.586 13H2a1 1 0 01-1-1V8a1 1 0 011-1h2.586l3.707-3.707a1 1 0 011.09-.217zM14.657 2.929a1 1 0 011.414 0A9.972 9.972 0 0119 10a9.972 9.972 0 01-2.929 7.071 1 1 0 01-1.414-1.414A7.971 7.971 0 0017 10c0-2.21-.894-4.208-2.343-5.657a1 1 0 010-1.414zm-2.829 2.828a1 1 0 011.415 0A5.983 5.983 0 0115 10a5.984 5.984 0 01-1.757 4.243 1 1 0 01-1.415-1.415A3.984 3.984 0 0013 10a3.983 3.983 0 00-1.172-2.828 1 1 0 010-1.415z" />
          </svg>
        </button>
        <input
          type="range"
          min="0"
          max="1"
          step="0.01"
          :value="volume"
          @input="$emit('volume', parseFloat($event.target.value))"
          class="w-20 h-1 bg-gray-700 rounded-full appearance-none cursor-pointer [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-3 [&::-webkit-slider-thumb]:h-3 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-white"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  currentSong: Object,
  isPlaying: Boolean,
  isLoading: Boolean,
  volume: { type: Number, default: 0.7 },
  progress: { type: Number, default: 0 },
  duration: { type: Number, default: 0 },
  currentTimeFormatted: String,
  durationFormatted: String
})

const emit = defineEmits(['toggle', 'prev', 'next', 'seek', 'volume'])

const defaultCover = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 48 48"%3E%3Crect fill="%23374151" width="48" height="48"/%3E%3Ctext x="24" y="28" text-anchor="middle" fill="%239CA3AF" font-size="20"%3E♪%3C/text%3E%3C/svg%3E'

const progressPercent = computed(() => {
  if (!props.duration) return 0
  return (props.progress / props.duration) * 100
})

const previousVolume = ref(props.volume)

const handleSeek = (e) => {
  const rect = e.currentTarget.getBoundingClientRect()
  const percent = (e.clientX - rect.left) / rect.width
  const time = percent * props.duration
  emit('seek', time)
}

const toggleMute = () => {
  if (props.volume > 0) {
    previousVolume.value = props.volume
    emit('volume', 0)
  } else {
    emit('volume', previousVolume.value || 0.7)
  }
}
</script>
