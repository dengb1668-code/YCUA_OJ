<template>
  <div class="problem-solution">
    <PostListPanel type="SOLUTION" :problem-id="problemId" />
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import PostListPanel from '../components/PostListPanel.vue'

const route = useRoute()
const problemId = Number(route.params.id)
</script>

<style scoped>
.problem-solution {
  background: var(--bg);
  min-height: calc(100vh - var(--header-height));
}
</style>
