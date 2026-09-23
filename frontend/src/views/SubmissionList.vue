<template>
  <div class="submission-list">
    <h2>提交记录</h2>
    <SubmissionListPanel mode="mine" />
  </div>
</template>

<script setup>
import SubmissionListPanel from '../components/SubmissionListPanel.vue'
</script>

<style scoped>
.submission-list {
  max-width: 1100px;
  margin: 0 auto;
  padding: 28px 16px 64px;
}

.submission-list h2 {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 16px;
}
</style>
