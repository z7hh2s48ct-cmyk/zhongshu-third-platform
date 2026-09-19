<template>
  <yd-form-picker
    :model-value="modelValue"
    :label="label"
    :label-width="labelWidth"
    :prop="prop"
    :disabled="disabled"
    :clearable="clearable"
    :columns="options"
    label-key="signature"
    value-key="id"
    :placeholder="placeholder"
    @update:model-value="handleUpdate"
    @confirm="handleConfirm"
    @clear="handleClear"
  />
</template>

<script lang="ts" setup>
import type { SmsChannel } from '@/api/system/sms/channel'
import { onMounted, ref } from 'vue'
import { getSimpleSmsChannelList } from '@/api/system/sms/channel'

const props = withDefaults(defineProps<{
  modelValue?: string
  label?: string
  labelWidth?: string
  placeholder?: string
  prop?: string
  disabled?: boolean
  clearable?: boolean
}>(), {
  label: '短信渠道',
  labelWidth: '200rpx',
  placeholder: '请选择短信渠道',
  prop: '',
  disabled: false,
  clearable: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string | undefined]
  'change': [item: SmsChannel | undefined]
}>()

const options = ref<SmsChannel[]>([]) // 短信渠道选项

/** 加载短信渠道选项 */
async function loadOptions() {
  if (options.value.length > 0) {
    return
  }
  options.value = await getSimpleSmsChannelList()
}

/** 更新短信渠道 */
function handleUpdate(value?: string) {
  emit('update:modelValue', value)
}

/** 选择短信渠道 */
function handleConfirm(value?: string) {
  emit('change', options.value.find(item => item.id === value))
}

/** 清空短信渠道 */
function handleClear() {
  emit('change', undefined)
}

/** 初始化 */
onMounted(() => {
  loadOptions()
})
</script>
