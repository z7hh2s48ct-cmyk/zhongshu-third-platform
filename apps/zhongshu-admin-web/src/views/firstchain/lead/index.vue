<template>
  <ContentWrap>
    <!-- 状态指标（三视角同源：权威状态列 GROUP BY，范围随服务端视角收敛） -->
    <el-row :gutter="12">
      <el-col v-for="s in LEAD_STATUS_OPTIONS" :key="s.value" :span="4">
        <el-card shadow="never" class="text-center" @click="filterByStatus(s.value)">
          <div class="text-20px font-bold">{{ metrics[s.value] ?? 0 }}</div>
          <div class="text-12px text-gray-400">{{ s.label }}</div>
        </el-card>
      </el-col>
      <el-col :span="4" class="text-right">
        <el-button
          v-hasPermi="['firstchain:lead:distribute']"
          type="primary"
          plain
          @click="openDistribute"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 下发线索
        </el-button>
      </el-col>
    </el-row>
  </ContentWrap>

  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" @submit.prevent>
      <el-form-item label="线索状态">
        <el-select v-model="queryParams.status" placeholder="全部" clearable class="!w-240px">
          <el-option v-for="s in LEAD_STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true">
      <el-table-column label="线索编号" align="left" prop="leadKey" width="200" />
      <el-table-column label="客户姓名" align="left" prop="customerName" width="120" />
      <el-table-column label="客户手机号" align="left" prop="customerPhone" width="130" />
      <el-table-column label="来源" align="left" prop="source" width="110" />
      <el-table-column label="归属组织" align="center" prop="orgId" width="100" />
      <el-table-column label="被分配员工" align="center" prop="assigneeUserId" width="100">
        <template #default="scope">{{ scope.row.assigneeUserId ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="110">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="版本" align="center" prop="version" width="70" />
      <el-table-column
        label="更新时间"
        align="center"
        prop="updateTime"
        width="180"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" align="center" width="330" fixed="right">
        <template #default="scope">
          <el-button
            v-hasPermi="['firstchain:lead:assign']"
            link
            type="primary"
            :disabled="scope.row.status !== 'DISTRIBUTED'"
            @click="openAssignDialog(scope.row, 'assign')"
          >
            分配
          </el-button>
          <el-button
            v-hasPermi="['firstchain:lead:claim']"
            link
            type="success"
            :disabled="scope.row.status !== 'ASSIGNED'"
            @click="handleClaim(scope.row)"
          >
            领取
          </el-button>
          <el-button
            v-hasPermi="['firstchain:lead:reassign']"
            link
            type="warning"
            :disabled="scope.row.status !== 'ASSIGNED'"
            @click="openAssignDialog(scope.row, 'reassign')"
          >
            改派
          </el-button>
          <el-button
            v-hasPermi="['firstchain:lead:followup']"
            link
            type="primary"
            :disabled="scope.row.status !== 'FOLLOWING'"
            @click="openFollowup(scope.row)"
          >
            跟进
          </el-button>
          <el-button
            v-hasPermi="['firstchain:lead:convert']"
            link
            type="success"
            :disabled="scope.row.status !== 'FOLLOWING'"
            @click="handleConvert(scope.row)"
          >
            转商机
          </el-button>
          <el-button
            v-hasPermi="['firstchain:lead:invalidate']"
            link
            type="danger"
            :disabled="scope.row.status !== 'FOLLOWING'"
            @click="openInvalidate(scope.row)"
          >
            无效
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 下发线索（平台运营；归属组织服务端写入 org_id） -->
  <Dialog v-model="distributeVisible" title="下发线索" width="520px">
    <el-form ref="distributeFormRef" :model="distributeForm" :rules="distributeRules" label-width="110px">
      <el-form-item label="客户姓名" prop="customerName">
        <el-input v-model="distributeForm.customerName" placeholder="必填（D-12 F1）" />
      </el-form-item>
      <el-form-item label="客户手机号" prop="customerPhone">
        <el-input v-model="distributeForm.customerPhone" placeholder="D-12 F2：员工视角默认脱敏" />
      </el-form-item>
      <el-form-item label="客户微信号" prop="customerWechat">
        <el-input v-model="distributeForm.customerWechat" />
      </el-form-item>
      <el-form-item label="客户详细地址" prop="customerAddress">
        <el-input v-model="distributeForm.customerAddress" />
      </el-form-item>
      <el-form-item label="线索来源" prop="source">
        <el-input v-model="distributeForm.source" placeholder="如：展会获客" />
      </el-form-item>
      <el-form-item label="归属组织编号" prop="orgId">
        <el-input-number v-model="distributeForm.orgId" :min="1" :controls="false" class="!w-180px" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="distributeVisible = false">取 消</el-button>
      <el-button type="primary" :loading="distributeLoading" @click="submitDistribute">确 定</el-button>
    </template>
  </Dialog>

  <!-- 分配 / 改派（负责人；目标员工归属由服务端校验） -->
  <Dialog v-model="assignVisible" :title="assignMode === 'assign' ? '分配线索' : '改派线索'" width="420px">
    <el-form label-width="110px">
      <el-form-item label="线索编号">
        <span>{{ assignRow?.leadKey }}</span>
      </el-form-item>
      <el-form-item label="目标员工" required>
        <el-select
          v-model="assignUserId"
          filterable
          :loading="memberLoading"
          placeholder="选择本组织在职员工"
          class="!w-240px"
        >
          <el-option v-for="m in memberOptions" :key="m.userId" :label="m.label" :value="m.userId" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="assignVisible = false">取 消</el-button>
      <el-button type="primary" :loading="assignLoading" @click="submitAssign">确 定</el-button>
    </template>
  </Dialog>

  <!-- 跟进记录（追加式，仅被分配员工本人） -->
  <Dialog v-model="followupVisible" title="追加跟进记录" width="520px">
    <el-form ref="followupFormRef" :model="followupForm" :rules="followupRules" label-width="90px">
      <el-form-item label="跟进时间" prop="followupTime">
        <el-date-picker
          v-model="followupForm.followupTime"
          type="datetime"
          value-format="x"
          placeholder="选择跟进时间"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="跟进内容" prop="content">
        <el-input v-model="followupForm.content" type="textarea" :rows="3" placeholder="必填（D-12 F1）" />
      </el-form-item>
      <el-form-item label="下一步计划" prop="nextStep">
        <el-input v-model="followupForm.nextStep" placeholder="如：周末上门量房" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="followupVisible = false">取 消</el-button>
      <el-button type="primary" :loading="followupLoading" @click="submitFollowup">提 交</el-button>
    </template>
  </Dialog>

  <!-- 无效关闭（原因枚举必填，OTHER 说明必填） -->
  <Dialog v-model="invalidateVisible" title="无效关闭" width="480px">
    <el-form ref="invalidateFormRef" :model="invalidateForm" :rules="invalidateRules" label-width="90px">
      <el-form-item label="关闭原因" prop="reasonName">
        <el-select v-model="invalidateForm.reasonName" placeholder="必选" class="!w-240px">
          <el-option v-for="r in INVALIDATE_REASONS" :key="r.value" :label="r.label" :value="r.value" />
        </el-select>
      </el-form-item>
      <el-form-item
        v-if="invalidateForm.reasonName === 'OTHER'"
        label="关闭说明"
        prop="reasonDetail"
      >
        <el-input v-model="invalidateForm.reasonDetail" type="textarea" :rows="2" placeholder="原因为「其他」时必填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="invalidateVisible = false">取 消</el-button>
      <el-button type="danger" :loading="invalidateLoading" @click="submitInvalidate">确认关闭</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as LeadApi from '@/api/firstchain/lead'
import type { LeadStatusMetrics, LeadVO } from '@/api/firstchain/lead'
import * as EmployeeApi from '@/api/firstchain/employee'

defineOptions({ name: 'FirstchainLead' })

const message = useMessage()

/** 线索五态（D-07 M6：DISTRIBUTED→ASSIGNED→FOLLOWING→CONVERTED/INVALID） */
const LEAD_STATUS_OPTIONS = [
  { label: '已下发', value: 'DISTRIBUTED' },
  { label: '已分配', value: 'ASSIGNED' },
  { label: '跟进中', value: 'FOLLOWING' },
  { label: '已转商机', value: 'CONVERTED' },
  { label: '已无效', value: 'INVALID' }
]
const statusLabel = (status: string) =>
  LEAD_STATUS_OPTIONS.find((s) => s.value === status)?.label ?? status
const statusTagType = (status: string): 'info' | 'warning' | 'primary' | 'success' | 'danger' => {
  switch (status) {
    case 'DISTRIBUTED':
      return 'info'
    case 'ASSIGNED':
      return 'warning'
    case 'FOLLOWING':
      return 'primary'
    case 'CONVERTED':
      return 'success'
    default:
      return 'danger'
  }
}

/** 无效关闭原因枚举（D-07 M7） */
const INVALIDATE_REASONS = [
  { label: '无法联系', value: 'UNREACHABLE' },
  { label: '预算不符', value: 'BUDGET_MISMATCH' },
  { label: '非目标客户', value: 'NOT_TARGET' },
  { label: '重复线索', value: 'DUPLICATE' },
  { label: '其他', value: 'OTHER' }
]

const loading = ref(true)
const total = ref(0)
const list = ref<LeadVO[]>([])
const metrics = ref<LeadStatusMetrics>({})
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  status: undefined as string | undefined
})

/** 查询列表 + 指标（视角范围由服务端解析，前端不声明归属/视角） */
const getList = async () => {
  loading.value = true
  try {
    const data = await LeadApi.getLeadPage(queryParams)
    list.value = data.list
    total.value = data.total
    metrics.value = (await LeadApi.getLeadStatusMetrics()) ?? {}
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}
const resetQuery = () => {
  queryParams.status = undefined
  handleQuery()
}
const filterByStatus = (status: string) => {
  queryParams.status = queryParams.status === status ? undefined : status
  handleQuery()
}

/** 操作后统一刷新（乐观锁冲突等服务端错误提示后以新版本基线重试） */
const refresh = async () => {
  await getList()
}

// ========== 下发线索（平台运营） ==========
const distributeVisible = ref(false)
const distributeLoading = ref(false)
const distributeFormRef = ref()
const distributeForm = reactive({
  customerName: '',
  customerPhone: '',
  customerWechat: '',
  customerAddress: '',
  source: '',
  orgId: undefined as number | undefined
})
const distributeRules = {
  customerName: [{ required: true, message: '客户姓名必填', trigger: 'blur' }],
  orgId: [{ required: true, message: '归属组织必填', trigger: 'blur' }]
}
const openDistribute = () => {
  distributeForm.customerName = ''
  distributeForm.customerPhone = ''
  distributeForm.customerWechat = ''
  distributeForm.customerAddress = ''
  distributeForm.source = ''
  distributeForm.orgId = undefined
  distributeVisible.value = true
}
const submitDistribute = async () => {
  const valid = await distributeFormRef.value?.validate().catch(() => false)
  if (!valid || !distributeForm.orgId) return
  distributeLoading.value = true
  try {
    await LeadApi.distributeLead({
      customerName: distributeForm.customerName,
      customerPhone: distributeForm.customerPhone || undefined,
      customerWechat: distributeForm.customerWechat || undefined,
      customerAddress: distributeForm.customerAddress || undefined,
      source: distributeForm.source || undefined,
      orgId: distributeForm.orgId
    })
    message.success('线索已下发')
    distributeVisible.value = false
    await refresh()
  } finally {
    distributeLoading.value = false
  }
}

// ========== 分配 / 改派（负责人） ==========
const assignVisible = ref(false)
const assignLoading = ref(false)
const assignMode = ref<'assign' | 'reassign'>('assign')
const assignRow = ref<LeadVO>()
const assignUserId = ref<number>()
const memberOptions = ref<{ userId: number; label: string }[]>([])
const memberLoading = ref(false)
const openAssignDialog = async (row: LeadVO, mode: 'assign' | 'reassign') => {
  assignRow.value = row
  assignMode.value = mode
  assignUserId.value = undefined
  assignVisible.value = true
  // 员工选择器数据源（本组织在职成员；对象级资格由服务端二次校验）
  memberOptions.value = []
  memberLoading.value = true
  try {
    const members = await EmployeeApi.listOrgMembers()
    memberOptions.value = members.map((m) => ({
      userId: m.userId,
      label: `${m.nickname}（${m.username} #${m.userId}）`
    }))
  } finally {
    memberLoading.value = false
  }
}
const submitAssign = async () => {
  if (!assignRow.value || !assignUserId.value) {
    message.warning('请填写员工用户编号')
    return
  }
  assignLoading.value = true
  try {
    if (assignMode.value === 'assign') {
      await LeadApi.assignLead({
        id: assignRow.value.id,
        assigneeUserId: assignUserId.value,
        expectedVersion: assignRow.value.version
      })
      message.success('已分配')
    } else {
      await LeadApi.reassignLead({
        id: assignRow.value.id,
        newAssigneeUserId: assignUserId.value,
        expectedVersion: assignRow.value.version
      })
      message.success('已改派')
    }
    assignVisible.value = false
    await refresh()
  } finally {
    assignLoading.value = false
  }
}

// ========== 领取（员工本人） ==========
const handleClaim = async (row: LeadVO) => {
  await message.confirm(`确认领取线索【${row.customerName ?? row.leadKey}】？`)
  await LeadApi.claimLead({ id: row.id, expectedVersion: row.version })
  message.success('已领取，可开始跟进')
  await refresh()
}

// ========== 跟进（追加式） ==========
const followupVisible = ref(false)
const followupLoading = ref(false)
const followupFormRef = ref()
const followupRow = ref<LeadVO>()
const followupForm = reactive({
  content: '',
  nextStep: '',
  followupTime: undefined as number | undefined
})
const followupRules = {
  content: [{ required: true, message: '跟进内容必填', trigger: 'blur' }],
  followupTime: [{ required: true, message: '跟进时间必填', trigger: 'blur' }]
}
const openFollowup = (row: LeadVO) => {
  followupRow.value = row
  followupForm.content = ''
  followupForm.nextStep = ''
  followupForm.followupTime = Date.now()
  followupVisible.value = true
}
const submitFollowup = async () => {
  const valid = await followupFormRef.value?.validate().catch(() => false)
  if (!valid || !followupRow.value) return
  followupLoading.value = true
  try {
    await LeadApi.followupLead({
      leadId: followupRow.value.id,
      content: followupForm.content,
      nextStep: followupForm.nextStep || undefined,
      followupTime: new Date(followupForm.followupTime!)
    })
    message.success('跟进记录已追加')
    followupVisible.value = false
    await refresh()
  } finally {
    followupLoading.value = false
  }
}

// ========== 转商机 ==========
const handleConvert = async (row: LeadVO) => {
  await message.confirm(`确认将线索【${row.customerName ?? row.leadKey}】转为有效商机？`)
  await LeadApi.convertLead({ leadId: row.id, expectedVersion: row.version })
  message.success('已转商机（终态锁定）')
  await refresh()
}

// ========== 无效关闭 ==========
const invalidateVisible = ref(false)
const invalidateLoading = ref(false)
const invalidateFormRef = ref()
const invalidateRow = ref<LeadVO>()
const invalidateForm = reactive({ reasonName: '', reasonDetail: '' })
const invalidateRules = {
  reasonName: [{ required: true, message: '关闭原因必选', trigger: 'blur' }],
  reasonDetail: [{ required: true, message: '原因为「其他」时说明必填', trigger: 'blur' }]
}
const openInvalidate = (row: LeadVO) => {
  invalidateRow.value = row
  invalidateForm.reasonName = ''
  invalidateForm.reasonDetail = ''
  invalidateVisible.value = true
}
const submitInvalidate = async () => {
  if (!invalidateRow.value) return
  const valid = await invalidateFormRef.value?.validate().catch(() => false)
  if (!valid) return
  invalidateLoading.value = true
  try {
    await LeadApi.invalidateLead({
      leadId: invalidateRow.value.id,
      reasonName: invalidateForm.reasonName,
      reasonDetail: invalidateForm.reasonDetail || undefined,
      expectedVersion: invalidateRow.value.version
    })
    message.success('已无效关闭（终态锁定）')
    invalidateVisible.value = false
    await refresh()
  } finally {
    invalidateLoading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>
