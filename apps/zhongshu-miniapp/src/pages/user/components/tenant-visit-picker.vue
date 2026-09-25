<template>
  <view v-if="$slots.default" @click="open">
    <slot :value="displayValue" />
  </view>

  <wd-select-picker
    ref="pickerRef"
    :model-value="pickerValue"
    title="切换租户"
    :columns="displayOptions"
    value-key="tenantId"
    label-key="displayName"
    type="radio"
    filterable
    root-portal
    :z-index="1100"
    :scroll-into-view="false"
    @confirm="handleConfirm"
  />
</template>

<script lang="ts" setup>
import type { SelectPickerInstance } from '@wot-ui/ui/components/wd-select-picker/types'
import type { VisitTenantOption } from '@/utils/tenant-visit'
import { computed, ref } from 'vue'
import { useUserStore } from '@/store/user'

// ZS-CLIENT-002.B：租户切换选择器（获批业务组织导航）。
// 选项由父组件从服务端「我的授权目标」构建后注入（服务端授权是唯一真相源，本组件不做加载 / 推导）；
// 登录项显示「当前登录」、当前访问项显示「当前访问」；确认后仅回传选项，切换执行与失败回滚由父组件负责。

const props = defineProps<{
  options: VisitTenantOption[]
}>()

const emit = defineEmits<{
  confirm: [option: VisitTenantOption]
}>()

const userStore = useUserStore()
const pickerRef = ref<SelectPickerInstance>() // 租户选择器
const pickerValue = computed(() => userStore.visitTenantId || userStore.tenantId || '')
const displayOptions = computed(() => props.options.map(option => ({
  ...option,
  displayName: option.isLoginTenant
    ? `${option.name}（当前登录）`
    : option.isCurrentVisit
      ? `${option.name}（当前访问）`
      : option.name,
})))
const displayValue = computed(() => {
  const currentId = userStore.visitTenantId ?? userStore.tenantId
  return props.options.find(option => String(option.tenantId) === String(currentId ?? ''))?.name
    || (currentId ? `租户 ${currentId}` : '未选择')
})

/** 打开租户选择器 */
function open() {
  pickerRef.value?.open()
}

/** 确认租户选择：回传选项，切换执行由父组件负责 */
function handleConfirm({ value }: { value: number | string }) {
  const option = props.options.find(item => item.tenantId === Number(value))
  if (option) {
    emit('confirm', option)
  }
}
</script>
