<template>
  <div class="scene-selector p-6">
    <h2 class="text-2xl font-bold text-white mb-6 text-center">选择你的场景</h2>
    <div class="grid grid-cols-2 md:grid-cols-3 gap-4">
      <div
        v-for="scene in scenes"
        :key="scene.id"
        @click="handleSelect(scene)"
        class="scene-card bg-gray-800 rounded-xl p-6 cursor-pointer transition-all duration-300 hover:bg-gray-700 hover:shadow-lg hover:scale-105 hover:shadow-purple-500/20 border border-gray-700 hover:border-purple-500/50"
      >
        <div class="text-5xl text-center mb-4">{{ scene.icon }}</div>
        <h3 class="text-lg font-semibold text-white text-center mb-2">{{ scene.name }}</h3>
        <p class="text-sm text-gray-400 text-center">{{ scene.description }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
const scenes = [
  { id: 'late-night', name: '深夜独处', icon: '🌙', description: '静谧的夜晚，适合独自思考', keywords: ['夜曲', '安静', '独处'] },
  { id: 'morning', name: '清晨唤醒', icon: '🌅', description: '美好的早晨，开启活力一天', keywords: ['清晨', '阳光', '活力'] },
  { id: 'focus', name: '工作专注', icon: '💼', description: '提升专注力，高效工作', keywords: ['专注', '轻音乐', '工作'] },
  { id: 'workout', name: '运动健身', icon: '🏃', description: '动感节拍，燃烧卡路里', keywords: ['运动', '动感', '节奏'] },
  { id: 'romantic', name: '浪漫约会', icon: '💕', description: '浪漫氛围，甜蜜时光', keywords: ['浪漫', '情歌', '甜蜜'] },
  { id: 'party', name: '派对狂欢', icon: '🎉', description: '热闹派对，尽情狂欢', keywords: ['派对', '欢快', '热门'] }
]

const emit = defineEmits(['select'])

const handleSelect = (scene) => {
  emit('select', scene)
}
</script>
