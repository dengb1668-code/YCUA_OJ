<template>
  <div ref="bgEl" class="vanta-bg" :style="{ background: bgColor }"></div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'

const props = defineProps({
  /** 背景色(hex 字符串, 如 '#ffffff' 或 '#07192f'), 同时作为特效加载前的占位色 */
  bgColor: { type: String, default: '#07192f' }
})

const bgEl = ref(null)
let effect = null

// 动态导入: three+vanta 约 600KB, 拆成独立 chunk 只在登录/注册/找回页加载
onMounted(async () => {
  const THREE = await import('three')
  // vanta 模块加载时读取 window.THREE(其 GPU 渲染路径闭包捕获的是全局变量), 必须先挂上再导入
  window.THREE = THREE
  const { default: BIRDS } = await import('vanta/dist/vanta.birds.min')
  effect = BIRDS({
    el: bgEl.value,
    THREE,
    mouseControls: true,
    touchControls: true,
    gyroControls: false,
    minHeight: 200,
    minWidth: 200,
    scale: 1.0,
    scaleMobile: 1.0,
    backgroundColor: parseInt(props.bgColor.slice(1), 16),
    color1: 0xff0000,
    color2: 0x00d1ff,
    colorMode: 'varianceGradient',
    birdSize: 1.0,
    wingSpan: 30.0,
    speedLimit: 5.0,
    separation: 20.0,
    alignment: 20.0,
    cohesion: 20.0,
    quantity: 5.0
  })
})

onUnmounted(() => {
  if (effect) effect.destroy()
})
</script>

<style scoped>
.vanta-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: -1;
  background: #07192f;
}
</style>
