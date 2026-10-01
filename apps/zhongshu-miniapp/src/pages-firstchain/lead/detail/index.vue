<template>
  <view class="yd-page-container">
    <wd-navbar title="线索详情" left-arrow placeholder safe-area-inset-top fixed @click-left="handleBack" />

    <!-- 基本信息 -->
    <wd-cell-group border>
      <wd-cell title="线索编号" :value="formData.leadKey || '-'" />
      <wd-cell title="客户姓名" :value="formData.customerName || '-'" />
      <wd-cell title="客户手机" :value="formData.customerPhone || '-'" />
      <wd-cell title="客户微信" :value="formData.customerWechat || '-'" />
      <wd-cell title="详细地址" :value="formData.customerAddress || '-'" />
      <wd-cell title="线索来源" :value="formData.source || '-'" />
      <wd-cell title="归属组织" :value="formData.orgId != null ? String(formData.orgId) : '-'" />
      <wd-cell title="被分配员工" :value="formData.assigneeUserId != null ? String(formData.assigneeUserId) : '未分配'" />
      <wd-cell title="状态">
        <wd-tag :type="statusTagType(formData.status)" custom-class="!leading-20px">
          {{ statusLabel(formData.status) }}
        </wd-tag>
      </wd-cell>
      <wd-cell title="版本" :value="formData.version != null ? String(formData.version) : '-'" />
      <wd-cell title="更新时间" :value="formatDateTime(formData.updateTime) || '-'" />
    </wd-cell-group>

    <!-- 底部操作条：按权限 + 状态显隐（动作权限由服务端最终判定） -->
    <view class="yd-detail-footer">
      <wd-button
        v-if="canClaim"
        block
        type="success"
        :loading="actionLoading"
        @click="handleClaim"
      >
        领取
      </wd-button>
      <wd-button
        v-if="canAssign"
        block
        type="primary"
        :loading="actionLoading"
        @click="openAssign"
      >
        分配
      </wd-button>
      <wd-button
        v-if="canReassign"
        block
        type="warning"
        :loading="actionLoading"
        @click="openAssign"
      >
        改派
      </wd-button>
      <wd-button
        v-if="canFollowup"
        block
        type="primary"
        :loading="actionLoading"
        @click="followupVisible = true"
      >
        跟进
      </wd-button>
      <wd-button
        v-if="canConvert"
        block
        type="success"
        :loading="actionLoading"
        @click="handleConvert"
      >
        转商机
      </wd-button>
      <wd-button
        v-if="canInvalidate"
        block
        type="error"
        :loading="actionLoading"
        @click="invalidateReasonVisible = true"
      >
        无效
      </wd-button>
    </view>

    <!-- 分配 / 改派：目标员工用户编号（归属由服务端校验） -->
    <wd-popup v-model="assignVisible" position="bottom" custom-class="rounded-t-24rpx p-32rpx" safe-area-inset-bottom>
      <view class="mb-24rpx text-32rpx font-bold">
        {{ isReassign ? '改派线索' : '分配线索' }}
      </view>
      <wd-picker
        v-model="assignUserIdInput"
        label="目标员工"
        :columns="memberColumns"
        :loading="memberLoading"
        placeholder="选择本组织在职员工"
      />
      <view class="mt-32rpx flex gap-20rpx">
        <wd-button plain block @click="assignVisible = false">
          取消
        </wd-button>
        <wd-button block type="primary" :loading="actionLoading" @click="submitAssign">
          确定
        </wd-button>
      </view>
    </wd-popup>

    <!-- 跟进记录（追加式；时间取提交时刻） -->
    <wd-popup v-model="followupVisible" position="bottom" custom-class="rounded-t-24rpx p-32rpx" safe-area-inset-bottom>
      <view class="mb-24rpx text-32rpx font-bold">
        追加跟进
      </view>
      <wd-textarea v-model="followupContent" label="跟进内容" placeholder="必填（如：电话沟通，客户有意向）" :maxlength="500" show-word-limit />
      <wd-input v-model="followupNextStep" label="下一步计划" placeholder="选填（如：周末上门量房）" />
      <view class="mt-32rpx flex gap-20rpx">
        <wd-button plain block @click="followupVisible = false">
          取消
        </wd-button>
        <wd-button block type="primary" :loading="actionLoading" @click="submitFollowup">
          提交
        </wd-button>
      </view>
    </wd-popup>

    <!-- 无效关闭：原因枚举（D-07 M7；OTHER 说明必填） -->
    <wd-action-sheet
      v-model="invalidateReasonVisible"
      :actions="invalidateReasonActions"
      title="选择关闭原因"
      @select="handleInvalidateReasonSelect"
    />
    <wd-popup v-model="invalidateDetailVisible" position="bottom" custom-class="rounded-t-24rpx p-32rpx" safe-area-inset-bottom>
      <view class="mb-24rpx text-32rpx font-bold">
        关闭说明（原因为「其他」时必填）
      </view>
      <wd-textarea v-model="invalidateDetail" :maxlength="200" show-word-limit />
      <view class="mt-32rpx flex gap-20rpx">
        <wd-button plain block @click="invalidateDetailVisible = false">
          取消
        </wd-button>
        <wd-button block type="error" :loading="actionLoading" @click="submitInvalidate">
          确认关闭
        </wd-button>
      </view>
    </wd-popup>
  </view>
</template>

<script lang="ts" setup>
import { useToast } from '@wot-ui/ui/components/wd-toast'
import { computed, onMounted, ref } from 'vue'
import type { Lead } from '@/api/firstchain/lead'
import {
  assignLead,
  claimLead,
  convertLead,
  followupLead,
  getLead,
  invalidateLead,
  LEAD_STATUS,
  reassignLead,
} from '@/api/firstchain/lead'
import { useAccess } from '@/hooks/useAccess'
import { navigateBackPlus } from '@/utils'
import { formatDateTime } from '@/utils/date'

const props = defineProps<{ id?: number | any }>()
definePage({
  style: {
    navigationBarTitleText: '',
    navigationStyle: 'custom',
  },
})

const toast = useToast()
const { hasAccessByCodes } = useAccess()

const formData = ref<Partial<Lead>>({})
const actionLoading = ref(false)
const leadId = computed(() => Number(props.id))

// 无效关闭原因（D-07 M7 枚举）
const INVALIDATE_REASONS = [
  { name: '无法联系', value: 'UNREACHABLE' },
  { name: '预算不符', value: 'BUDGET_MISMATCH' },
  { name: '非目标客户', value: 'NOT_TARGET' },
  { name: '重复线索', value: 'DUPLICATE' },
  { name: '其他', value: 'OTHER' },
]

function statusLabel(status?: string) {
  switch (status) {
    case LEAD_STATUS.DISTRIBUTED:
      return '已下发'
    case LEAD_STATUS.ASSIGNED:
      return '已分配'
    case LEAD_STATUS.FOLLOWING:
      return '跟进中'
    case LEAD_STATUS.CONVERTED:
      return '已转商机'
    case LEAD_STATUS.INVALID:
      return '已无效'
    default:
      return '-'
  }
}
function statusTagType(status?: string): 'primary' | 'success' | 'danger' | 'warning' | 'default' {
  switch (status) {
    case LEAD_STATUS.ASSIGNED:
      return 'warning'
    case LEAD_STATUS.FOLLOWING:
      return 'primary'
    case LEAD_STATUS.CONVERTED:
      return 'success'
    case LEAD_STATUS.INVALID:
      return 'danger'
    default:
      return 'default'
  }
}

const status = computed(() => formData.value.status)
const canClaim = computed(() =>
  status.value === LEAD_STATUS.ASSIGNED && hasAccessByCodes(['firstchain:lead:claim']))
const canAssign = computed(() =>
  status.value === LEAD_STATUS.DISTRIBUTED && hasAccessByCodes(['firstchain:lead:assign']))
const isReassign = computed(() => status.value === LEAD_STATUS.ASSIGNED && hasAccessByCodes(['firstchain:lead:reassign']))
const canReassign = computed(() => isReassign.value && !canAssign.value)
const canFollowup = computed(() =>
  status.value === LEAD_STATUS.FOLLOWING && hasAccessByCodes(['firstchain:lead:followup']))
const canConvert = computed(() =>
  status.value === LEAD_STATUS.FOLLOWING && hasAccessByCodes(['firstchain:lead:convert']))
const canInvalidate = computed(() =>
  status.value === LEAD_STATUS.FOLLOWING && hasAccessByCodes(['firstchain:lead:invalidate']))

/** 加载详情（越权由服务端显式拒绝） */
async function loadDetail() {
  toast.loading('加载中…')
  try {
    formData.value = await getLead(leadId.value)
  } finally {
    toast.close()
  }
}

/** 领取（员工本人；乐观锁冲突以服务端提示为准后刷新基线） */
async function handleClaim() {
  actionLoading.value = true
  try {
    await claimLead({ id: leadId.value, expectedVersion: formData.value.version! })
    toast.show('已领取，可开始跟进')
    await loadDetail()
  } finally {
    actionLoading.value = false
  }
}

// ========== 分配 / 改派（本组织在职成员选择器） ==========
const assignVisible = ref(false)
const assignUserIdInput = ref('')
const memberColumns = ref<{ label: string, value: string }[]>([])
const memberLoading = ref(false)
async function openAssign() {
  assignVisible.value = true
  memberColumns.value = []
  memberLoading.value = true
  try {
    const members = await listOrgMembers()
    memberColumns.value = members.map(m => ({
      label: `${m.nickname}（${m.username} #${m.userId}）`,
      value: String(m.userId),
    }))
  } finally {
    memberLoading.value = false
  }
}
async function submitAssign() {
  const userId = Number(assignUserIdInput.value)
  if (!userId || userId <= 0) {
    toast.show('请选择目标员工')
    return
  }
  actionLoading.value = true
  try {
    if (status.value === LEAD_STATUS.DISTRIBUTED) {
      await assignLead({ id: leadId.value, assigneeUserId: userId, expectedVersion: formData.value.version! })
      toast.show('已分配')
    } else {
      await reassignLead({ id: leadId.value, newAssigneeUserId: userId, expectedVersion: formData.value.version! })
      toast.show('已改派')
    }
    assignVisible.value = false
    assignUserIdInput.value = ''
    await loadDetail()
  } finally {
    actionLoading.value = false
  }
}

// ========== 跟进（追加式；时间取提交时刻） ==========
const followupVisible = ref(false)
const followupContent = ref('')
const followupNextStep = ref('')
async function submitFollowup() {
  if (!followupContent.value.trim()) {
    toast.show('跟进内容必填')
    return
  }
  actionLoading.value = true
  try {
    await followupLead({
      leadId: leadId.value,
      content: followupContent.value.trim(),
      nextStep: followupNextStep.value.trim() || undefined,
      followupTime: Date.now(),
    })
    toast.show('跟进记录已追加')
    followupVisible.value = false
    followupContent.value = ''
    followupNextStep.value = ''
    await loadDetail()
  } finally {
    actionLoading.value = false
  }
}

// ========== 转商机 ==========
async function handleConvert() {
  actionLoading.value = true
  try {
    await convertLead({ leadId: leadId.value, expectedVersion: formData.value.version! })
    toast.show('已转商机（终态锁定）')
    await loadDetail()
  } finally {
    actionLoading.value = false
  }
}

// ========== 无效关闭 ==========
const invalidateReasonVisible = ref(false)
const invalidateDetailVisible = ref(false)
const invalidateDetail = ref('')
const invalidateReasonActions = INVALIDATE_REASONS.map(r => ({ name: r.name }))
let pendingInvalidateReason = ''
const invalidateReasonActions_ = INVALIDATE_REASONS
function handleInvalidateReasonSelect(item: { name: string }) {
  const reason = invalidateReasonActions_.find(r => r.name === item.name)
  if (!reason) {
    return
  }
  pendingInvalidateReason = reason.value
  if (reason.value === 'OTHER') {
    invalidateDetail.value = ''
    invalidateDetailVisible.value = true
  } else {
    submitInvalidate()
  }
}
async function submitInvalidate() {
  if (pendingInvalidateReason === 'OTHER' && !invalidateDetail.value.trim()) {
    toast.show('原因为「其他」时说明必填')
    return
  }
  actionLoading.value = true
  try {
    await invalidateLead({
      leadId: leadId.value,
      reasonName: pendingInvalidateReason,
      reasonDetail: invalidateDetail.value.trim() || undefined,
      expectedVersion: formData.value.version!,
    })
    toast.show('已无效关闭（终态锁定）')
    invalidateDetailVisible.value = false
    invalidateReasonVisible.value = false
    await loadDetail()
  } finally {
    actionLoading.value = false
  }
}

function handleBack() {
  navigateBackPlus()
}

onMounted(async () => {
  await loadDetail()
  // 操作后返回列表页刷新（事件总线，循 pages-bpm 先例）
})
</script>
