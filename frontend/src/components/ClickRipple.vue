<template>
  <div class="click-ripple-root"></div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'

const MAX_ACTIVE = 30
let active = 0

/** 在 (x, y) 生成一次水面波纹特效 */
function spawn(x, y) {
  if (active >= MAX_ACTIVE) return
  active++

  const wrap = document.createElement('div')
  wrap.className = 'w-wrap'
  wrap.style.left = x + 'px'
  wrap.style.top = y + 'px'

  // 波纹容器(平行于屏幕的正圆扩散)
  const surface = document.createElement('div')
  surface.className = 'w-surface'
  for (let i = 0; i < 3; i++) {
    surface.appendChild(document.createElement('span')).className = 'w-ring'
  }

  wrap.appendChild(surface)
  document.body.appendChild(wrap)

  setTimeout(() => {
    wrap.remove()
    active--
  }, 1200)
}

function onClick(e) {
  if (e.button !== 0) return
  spawn(e.clientX, e.clientY)
}

onMounted(() => {
  document.addEventListener('pointerdown', onClick)
})

onUnmounted(() => {
  document.removeEventListener('pointerdown', onClick)
})
</script>

<style>
/* 全局特效样式(元素运行时动态创建, 不能加 scoped) */
.w-wrap {
  position: fixed;
  z-index: 9999;
  pointer-events: none;
  width: 0;
  height: 0;
}

/* 波纹容器: 与屏幕平行的正圆 */
.w-surface {
  position: absolute;
  left: -60px;
  top: -60px;
  width: 120px;
  height: 120px;
}

.w-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 2px solid rgba(26, 92, 200, 0.5);
  animation: w-wave 1.1s ease-out forwards;
}

.w-ring:nth-child(2) {
  animation-delay: 0.13s;
  border-color: rgba(77, 163, 255, 0.45);
}

.w-ring:nth-child(3) {
  animation-delay: 0.26s;
  border-color: rgba(0, 209, 255, 0.35);
}

@keyframes w-wave {
  0% {
    transform: scale(0.06);
    opacity: 0.9;
  }
  100% {
    transform: scale(1);
    opacity: 0;
  }
}
</style>
