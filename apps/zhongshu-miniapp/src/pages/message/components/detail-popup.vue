<template>
  <wd-popup
    v-model="visible"
    position="bottom"
    custom-style="border-radius: 24rpx 24rpx 0 0; height: 50%"
    safe-area-inset-bottom
    @close="visible = false"
  >
    <view class="h-full flex flex-col p-32rpx">
      <!-- 标题 -->
      <view class="mb-32rpx flex items-center justify-between">
        <view class="text-36rpx text-[#333] font-semibold">
          消息详情
        </view>
        <view class="p-8rpx" @click="visible = false">
          <wd-icon name="close" size="20px" color="#999" />
        </view>
      </view>

      <!-- 详情内容 -->
      <view v-if="formData" class="flex flex-1 flex-col overflow-hidden space-y-24rpx">
        <view class="flex items-start">
          <text class="w-160rpx shrink-0 text-28rpx text-[#999]">发送人</text>
          <text class="text-28rpx text-[#333]">{{ formData.templateNickname }}</text>
        </view>
        <view class="flex items-start">
          <text class="w-160rpx shrink-0 text-28rpx text-[#999]">发送时间</text>
          <text class="text-28rpx text-[#333]">{{ formatDateTime(formData.createTime) }}</text>
        </view>
        <view class="flex items-start">
          <text class="w-160rpx shrink-0 text-28rpx text-[#999]">消息类型</text>
          <text class="text-28rpx text-[#333]">
            {{ getDictLabel(DICT_TYPE.SYSTEM_NOTIFY_TEMPLATE_TYPE, formData.templateType) }}
          </text>
        </view>
        <view class="flex items-start">
          <text class="w-160rpx shrink-0 text-28rpx text-[#999]">是否已读</text>
          <wd-tag v-if="formData.readStatus" type="success" variant="plain">
            已读
          </wd-tag>
          <wd-tag v-else type="warning" variant="plain">
            未读
          </wd-tag>
        </view>
        <view v-if="formData.readStatus" class="flex items-start">
          <text class="w-160rpx shrink-0 text-28rpx text-[#999]">阅读时间</text>
          <text class="text-28rpx text-[#333]">{{ formatDateTime(formData.readTime) || '-' }}</text>
        </view>
        <view class="flex flex-1 flex-col overflow-hidden">
          <text class="mb-12rpx w-160rpx shrink-0 text-28rpx text-[#999]">消息内容</text>
          <view class="flex-1 rounded-12rpx bg-[#f5f5f5] p-24rpx">
            <text class="text-28rpx text-[#333]">{{ formData.templateContent }}</text>
          </view>
        </view>
        <!-- ZS-MSG-003：跳转前经服务端落点二次授权；不可用明确提示，不猜测跳转 -->
        <wd-button block :loading="landingLoading" @click="goLanding">
          前往处理
        </wd-button>
      </view>
    </view>
  </wd-popup>
</template>

<script lang="ts" setup>
import type { NotifyMessage } from '@/api/system/notify/message'
import { ref } from 'vue'
import { resolveNotifyMessageLanding } from '@/api/system/notify/message'
import { useToast } from '@wot-ui/ui/components/wd-toast'
import { getDictLabel } from '@/hooks/useDict'
import { isTabBarPage } from '@/tabbar/config'
import { DICT_TYPE } from '@/utils/constants'
import { formatDateTime } from '@/utils/date'
import { parseUrl, setTabParams } from '@/utils/url'
import {
  applyNotifyLandingRoute,
  notifyLandingUnavailableText,
} from '../landing'

const visible = ref(false) // 详情弹窗显示状态
const formData = ref<NotifyMessage>() // 详情数据
const landingLoading = ref(false) // 落点解析中

/** 打开弹窗 */
function open(data: NotifyMessage) {
  formData.value = data
  visible.value = true
}

/** 关闭弹窗 */
function close() {
  visible.value = false
}

/** 前往处理：解析落点（服务端二次授权）→ 导航由既有路由守卫最终判定 */
const toast = useToast()
async function goLanding() {
  if (!formData.value || landingLoading.value) {
    return
  }
  landingLoading.value = true
  try {
    const result = await resolveNotifyMessageLanding(formData.value.id, 'MOBILE')
    // r0-P2：TabBar 落点须 switchTab（navigateTo 会拒绝），query 经 globalData 透传；
    // 导航失败（页面不存在/非法目标）明确反馈，不静默
    if (applyNotifyLandingRoute((url) => {
      visible.value = false
      const { path, query } = parseUrl(url)
      const fail = () => toast.show('落点页面不存在或不可达')
      if (isTabBarPage(path)) {
        if (Object.keys(query).length > 0) {
          setTabParams(query)
        }
        uni.switchTab({ url: path, fail })
      } else {
        uni.navigateTo({ url, fail })
      }
    }, result) === 'unavailable') {
      toast.show(notifyLandingUnavailableText(result))
    }
  } catch {
    // 他人消息/不存在等安全拒绝已由请求层统一 toast（http.ts），此处不再重复提示
  } finally {
    landingLoading.value = false
  }
}

defineExpose({ open, close })
</script>
