<template>
  <div class="problem-submissions">
    <div class="head">
      <el-page-header @back="router.back()" />
      <h2>本题提交记录</h2>
    </div>
    <SubmissionListPanel mode="problem" :problem-id="problemId" />
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import SubmissionListPanel from '../components/SubmissionListPanel.vue'

const route = useRoute()
const router = useRouter()
const problemId = Number(route.params.id)
</script>

<style scoped>
.problem-submissions {
  background: var(--bg);
  min-height: calc(100vh - var(--header-height));
  max-width: 1100px;
  margin: 0 auto;
  padding: 16px 16px 60px;
}

.head {
  margin-bottom: 16px;
}

.head h2 {
  font-size: 20px;
  font-weight: 700;
  margin: 8px 0 0;
}
</style>
