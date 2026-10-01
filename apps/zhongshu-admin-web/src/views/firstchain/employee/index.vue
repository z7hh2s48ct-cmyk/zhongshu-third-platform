<template>
  <ContentWrap>
    <el-alert type="info" :closable="false" class="mb-15px">
      <template #title>
        负责人直接创建员工账号（D-07 M5-A）：员工只进本组织并绑定员工默认授权；停用账号/任职即失权。
      </template>
    </el-alert>
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="110px"
      class="max-w-520px"
      @submit.prevent
    >
      <el-form-item label="登录用户名" prop="username">
        <el-input v-model="form.username" placeholder="4~30 位字母数字（全平台唯一，统一小写存储）" />
      </el-form-item>
      <el-form-item label="员工姓名" prop="nickname">
        <el-input v-model="form.nickname" placeholder="展示昵称" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">创建账号</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 创建结果：初始密码一次性下发 -->
  <Dialog v-model="resultVisible" title="员工账号已创建" width="480px">
    <el-result icon="success" :title="`账号：${result?.username}`" :sub-title="`用户编号：${result?.userId}`">
      <template #extra>
        <div class="text-left">
          <el-alert type="warning" :closable="false" class="mb-10px">
            <template #title>
              初始密码仅本次展示（明文不落审计/日志），请复制交由员工本人并提醒尽快修改（强制首改后置登记）。
            </template>
          </el-alert>
          <el-input v-model="result!.initialPassword" readonly>
            <template #append>
              <el-button @click="copyInitialPassword">复制</el-button>
            </template>
          </el-input>
        </div>
      </template>
    </el-result>
    <template #footer>
      <el-button type="primary" @click="resultVisible = false">我已保存</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import * as EmployeeApi from '@/api/firstchain/employee'
import type { EmployeeCreatedRespVO } from '@/api/firstchain/employee'

defineOptions({ name: 'FirstchainEmployee' })

const message = useMessage()

const loading = ref(false)
const formRef = ref()
const form = reactive({ username: '', nickname: '' })
const rules = {
  username: [
    { required: true, message: '登录用户名必填', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9]{4,30}$/,
      message: '用户名须为 4~30 位字母数字',
      trigger: 'blur'
    }
  ],
  nickname: [{ required: true, message: '员工姓名必填', trigger: 'blur' }]
}

const resultVisible = ref(false)
const result = ref<EmployeeCreatedRespVO>()

const submit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    result.value = await EmployeeApi.createEmployee({
      username: form.username,
      nickname: form.nickname
    })
    resultVisible.value = true
    form.username = ''
    form.nickname = ''
  } finally {
    loading.value = false
  }
}

const copyInitialPassword = async () => {
  if (!result.value?.initialPassword) return
  await navigator.clipboard.writeText(result.value.initialPassword)
  message.success('已复制初始密码')
}
</script>
