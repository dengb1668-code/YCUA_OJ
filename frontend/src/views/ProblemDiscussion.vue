<template>
  <div class="problem-discussion">
    <PostListPanel type="DISCUSSION" :problem-id="problemId" />
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import PostListPanel from '../components/PostListPanel.vue'

const route = useRoute()
const problemId = Number(route.params.id)
</script>

<style scoped>
.problem-discussion {
  background: #fff;
  min-height: calc(100vh - 44px);
}
</style>
