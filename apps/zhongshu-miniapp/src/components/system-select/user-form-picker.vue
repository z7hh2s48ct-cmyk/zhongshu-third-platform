<template>
  <UserPicker
    ref="pickerRef"
    :model-value="Array.isArray(modelValue) ? modelValue.map(String) : modelValue != null ? String(modelValue) : undefined"
    :type="type"
    :title="label || placeholder"
    :disabled="disabled"
    :hide-ids="hideIds"
    :disabled-ids="disabledIds"
    :disabled-text="disabledText"
    @update:model-value="handleUpdate"
    @confirm="handleConfirm"
  >
    <template #default="{ value }">
      <wd-form-item
        :title="label"
        :title-width="labelWidth"
        :prop="prop || undefined"
        :is-link="!disabled"
        :value="value"
        :placeholder="placeholder"
      />
    </template>
  </UserPicker>
</template>

<script lang="ts" setup>
import type { User } from '@/api/system/user'
import { ref } from 'vue'
import UserPicker from './user-picker.vue'

const props = withDefaults(defineProps<{
  modelValue?: number | number[]
  type?: 'radio' | 'checkbox'
  label?: string
  labelWidth?: string
  placeholder?: string
  prop?: string
  disabled?: boolean
  hideIds?: string[]
  disabledIds?: string[]
  disabledText?: string
}>(), {
  type: 'radio',
  label: '用户',
  labelWidth: '180rpx',
  placeholder: '请选择用户',
  prop: '',
  disabled: false,
  hideIds: () => [],
  disabledIds: () => [],
  disabledText: '不可选择',
})

const emit = defineEmits<{
  'update:modelValue': [value: number | number[] | undefined]
  'confirm': [users: User[]]
}>()

const pickerRef = ref<InstanceType<typeof UserPicker>>() // 用户选择器

/** 更新用户编号 */
function handleUpdate(value: string | string[] | undefined) {
  emit('update:modelValue', Array.isArray(value) ? value.map(Number) : value != null ? Number(value) : undefined)
}

/** 确认用户选择 */
function handleConfirm(users: User[]) {
  emit('confirm', users)
}

/** 格式化用户编号 */
function format(value?: number | number[]) {
  return arguments.length > 0 ? pickerRef.value?.format(value != null ? String(value) : undefined) || '' : pickerRef.value?.format() || ''
}

defineExpose({ format })
</script>
