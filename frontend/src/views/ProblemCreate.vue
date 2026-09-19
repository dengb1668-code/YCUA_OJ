<template>
  <div class="problem-create">
    <div class="container">
      <h2>创建题目</h2>
      <ProblemForm submit-text="创建题目" :submitting="submitting" @submit="handleCreate" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProblemForm from '../components/ProblemForm.vue'
import { createProblem } from '../api/problem'

const router = useRouter()

const submitting = ref(false)

async function handleCreate(payload) {
  submitting.value = true
  try {
    const id = await createProblem(payload)
    ElMessage.success('题目创建成功')
    // 引导去配置测试数据(没有测试点时判题会退化为按样例比对)
    try {
      await ElMessageBox.confirm('是否立即配置测试数据?', '题目创建成功', {
        confirmButtonText: '去配置测试数据',
        cancelButtonText: '先看看题目',
        type: 'info'
      })
      router.push(`/problems/${id}/manage`)
    } catch (e) {
      router.push(`/problems/${id}`)
    }
  } catch (e) {
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.problem-create {
  min-height: calc(100vh - 44px);
}

.container {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 16px 80px;
}

.problem-create h2 {
  font-size: 20px;
  font-weight: normal;
  margin: 0 0 16px;
}
</style>
