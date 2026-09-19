<template>
  <yd-search-picker
    ref="pickerRef"
    :model-value="modelValue"
    :label="label"
    :columns="options"
    label-key="signature"
    value-key="id"
    :placeholder="placeholder"
    @update:model-value="handleUpdate"
  />
</template>

<script lang="ts" setup>
import type { YdSearchPickerExpose } from '@/components/zszj-ui'
import type { SmsChannel } from '@/api/system/sms/channel'
import { onMounted, ref } from 'vue'
import { getSimpleSmsChannelList } from '@/api/system/sms/channel'

const props = withDefaults(defineProps<{
  modelValue?: string
  label?: string
  placeholder?: string
}>(), {
  label: '短信渠道',
  placeholder: '请选择短信渠道',
})

const emit = defineEmits<{
  'update:modelValue': [value: string | undefined]
  'change': [item: SmsChannel | undefined]
}>()

const pickerRef = ref<YdSearchPickerExpose>() // 通用搜索选择器
const options = ref<SmsChannel[]>([]) // 短信渠道选项

/** 更新短信渠道 */
function handleUpdate(value?: string) {
  emit('update:modelValue', value)
  emit('change', options.value.find(item => item.id === value))
}

/** 格式化短信渠道 */
function format(value?: string) {
  return pickerRef.value?.format(value) || (value == null ? '' : String(value))
}

defineExpose({ format })

/** 初始化 */
onMounted(async () => {
  options.value = await getSimpleSmsChannelList()
})
</script>
