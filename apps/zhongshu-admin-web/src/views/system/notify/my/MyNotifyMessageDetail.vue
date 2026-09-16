<template>
  <Dialog v-model="dialogVisible" :max-height="500" :scroll="true" title="消息详情">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="发送人">
        {{ detailData.templateNickname }}
      </el-descriptions-item>
      <el-descriptions-item label="发送时间">
        {{ formatDate(detailData.createTime) }}
      </el-descriptions-item>
      <el-descriptions-item label="消息类型">
        <dict-tag :type="DICT_TYPE.SYSTEM_NOTIFY_TEMPLATE_TYPE" :value="detailData.templateType" />
      </el-descriptions-item>
      <el-descriptions-item label="是否已读">
        <dict-tag :type="DICT_TYPE.INFRA_BOOLEAN_STRING" :value="detailData.readStatus" />
      </el-descriptions-item>
      <el-descriptions-item v-if="detailData.readStatus" label="阅读时间">
        {{ formatDate(detailData.readTime) }}
      </el-descriptions-item>
      <el-descriptions-item label="内容">
        {{ detailData.templateContent }}
      </el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <!-- ZS-MSG-003：跳转前经服务端落点二次授权；不可用明确提示，不猜测跳转 -->
      <el-button :disabled="!detailData.id" type="primary" @click="goLanding"> 前往处理 </el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as NotifyMessageApi from '@/api/system/notify/message'
import {
  applyNotifyLandingRoute,
  notifyLandingUnavailableText
} from '@/views/system/notify/landing'

defineOptions({ name: 'MyNotifyMessageDetailDetail' })

const dialogVisible = ref(false) // 弹窗的是否展示
const detailLoading = ref(false) // 表单的加载中
const detailData = ref({} as NotifyMessageApi.NotifyMessageVO) // 详情数据

/** 打开弹窗 */
const open = async (data: NotifyMessageApi.NotifyMessageVO) => {
  dialogVisible.value = true
  // 设置数据
  detailLoading.value = true
  try {
    detailData.value = data
  } finally {
    detailLoading.value = false
  }
}

/** 前往处理：解析落点（服务端二次授权）→ 导航由既有路由守卫最终判定 */
const message = useMessage()
const { push } = useRouter()
const goLanding = async () => {
  try {
    const result = await NotifyMessageApi.resolveNotifyMessageLanding(detailData.value.id, 'WEB')
    if (applyNotifyLandingRoute({ push }, result) === 'unavailable') {
      message.warning(notifyLandingUnavailableText(result))
      return
    }
    dialogVisible.value = false
  } catch {
    // 安全拒绝（他人消息/不存在）已由 axios 拦截层统一错误提示，此处不再重复
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗
</script>
