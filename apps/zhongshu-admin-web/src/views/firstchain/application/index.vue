<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form :inline="true" :model="queryParams" class="-mb-15px" @submit.prevent>
      <el-form-item label="申请状态">
        <el-select v-model="queryParams.status" placeholder="全部" clearable class="!w-240px">
          <el-option v-for="s in APPLICATION_STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
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
      <el-table-column label="申请编号" align="left" prop="appKey" width="220" />
      <el-table-column label="申请方名称" align="left" prop="applicantName" min-width="160" />
      <el-table-column label="联系人" align="left" prop="contactName" width="120" />
      <el-table-column label="联系人电话" align="left" prop="contactPhone" width="140" />
      <el-table-column label="状态" align="center" prop="status" width="110">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="版本" align="center" prop="version" width="70" />
      <el-table-column
        label="提交时间"
        align="center"
        prop="createTime"
        width="180"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" align="center" width="200" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row)">详情</el-button>
          <el-button
            v-hasPermi="['firstchain:application:approve']"
            link
            type="success"
            :disabled="scope.row.status !== 'SUBMITTED'"
            @click="openAudit(scope.row, true)"
          >
            通过
          </el-button>
          <el-button
            v-hasPermi="['firstchain:application:reject']"
            link
            type="danger"
            :disabled="scope.row.status !== 'SUBMITTED'"
            @click="openAudit(scope.row, false)"
          >
            拒绝
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

  <!-- 详情对话框 -->
  <Dialog v-model="detailVisible" title="申请详情" width="600px">
    <el-descriptions v-if="detailRow" :column="1" border>
      <el-descriptions-item label="申请编号">{{ detailRow.appKey }}</el-descriptions-item>
      <el-descriptions-item label="申请方名称">{{ detailRow.applicantName }}</el-descriptions-item>
      <el-descriptions-item label="联系人">{{ detailRow.contactName }}</el-descriptions-item>
      <el-descriptions-item label="联系人电话">{{ detailRow.contactPhone }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="statusTagType(detailRow.status)">{{ statusLabel(detailRow.status) }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item v-if="detailRow.rejectReason" label="拒绝意见">
        {{ detailRow.rejectReason }}
      </el-descriptions-item>
      <el-descriptions-item label="资质附件（FILE 私有）">
        {{ detailRow.attachmentFileIds?.length ? detailRow.attachmentFileIds.join('、') : '无' }}
      </el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="detailVisible = false">关 闭</el-button>
    </template>
  </Dialog>

  <!-- 审批对话框（通过可空意见 / 拒绝意见必填） -->
  <Dialog v-model="auditVisible" :title="auditPass ? '审批通过并开通' : '审批拒绝'" width="480px">
    <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="90px">
      <el-form-item label="申请编号">
        <span>{{ auditRow?.appKey }}</span>
      </el-form-item>
      <el-form-item label="审批意见" prop="reason">
        <el-input
          v-model="auditForm.reason"
          type="textarea"
          :rows="3"
          :placeholder="auditPass ? '可填审批意见（开通结果将通知提交人与负责人）' : '必填：请填写拒绝意见'"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="auditLoading" @click="auditVisible = false">取 消</el-button>
      <el-button type="primary" :loading="auditLoading" @click="submitAudit">确 定</el-button>
    </template>
  </Dialog>

  <!-- 开通结果对话框（初始密码一次性下发，强制首改后置登记） -->
  <Dialog v-model="openingVisible" title="开通成功" width="480px">
    <el-result icon="success" title="加盟商已开通" :sub-title="`组织编号：${openingResult?.organizationId}`">
      <template #extra>
        <div class="text-left">
          <el-alert type="warning" :closable="false" class="mb-10px">
            <template #title>
              初始密码仅本次展示（明文不落审计/日志），请复制交由加盟商负责人并在首次登录后修改。
            </template>
          </el-alert>
          <el-input v-model="openingResult!.initialPassword" readonly>
            <template #append>
              <el-button @click="copyInitialPassword">复制</el-button>
            </template>
          </el-input>
        </div>
      </template>
    </el-result>
    <template #footer>
      <el-button type="primary" @click="openingVisible = false">我已保存</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as ApplicationApi from '@/api/firstchain/application'
import type { ApplicationVO } from '@/api/firstchain/application'

defineOptions({ name: 'FirstchainApplication' })

const message = useMessage()

/** 申请三态（M3：DRAFT→SUBMITTED→APPROVED/REJECTED） */
const APPLICATION_STATUS_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已提交', value: 'SUBMITTED' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' }
]
const statusLabel = (status: string) =>
  APPLICATION_STATUS_OPTIONS.find((s) => s.value === status)?.label ?? status
const statusTagType = (status: string): 'info' | 'warning' | 'success' | 'danger' => {
  switch (status) {
    case 'SUBMITTED':
      return 'warning'
    case 'APPROVED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

const loading = ref(true)
const total = ref(0)
const list = ref<ApplicationVO[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  status: undefined as string | undefined
})

/** 查询列表（视角范围由服务端解析：平台=授权范围） */
const getList = async () => {
  loading.value = true
  try {
    const data = await ApplicationApi.getApplicationPage(queryParams)
    list.value = data.list
    total.value = data.total
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

// ========== 详情 ==========
const detailVisible = ref(false)
const detailRow = ref<ApplicationVO>()
const openDetail = (row: ApplicationVO) => {
  detailRow.value = row
  detailVisible.value = true
}

// ========== 审批（通过/拒绝；拒绝意见必填） ==========
const auditVisible = ref(false)
const auditLoading = ref(false)
const auditPass = ref(true)
const auditRow = ref<ApplicationVO>()
const auditFormRef = ref()
const auditForm = reactive({ reason: '' })
const auditRules = {
  reason: [{ required: true, message: '拒绝意见必填', trigger: 'blur' }]
}
const openAudit = (row: ApplicationVO, pass: boolean) => {
  auditRow.value = row
  auditPass.value = pass
  auditForm.reason = ''
  // 通过时意见可空：动态移除必填校验
  auditRules.reason[0].required = !pass
  auditVisible.value = true
}
const submitAudit = async () => {
  if (!auditRow.value) return
  if (!auditPass.value) {
    const valid = await auditFormRef.value?.validate().catch(() => false)
    if (!valid) return
  }
  auditLoading.value = true
  try {
    if (auditPass.value) {
      const opening = await ApplicationApi.approveApplication({
        appKey: auditRow.value.appKey,
        reason: auditForm.reason || undefined
      })
      auditVisible.value = false
      openingResult.value = opening
      openingVisible.value = true
    } else {
      await ApplicationApi.rejectApplication({
        appKey: auditRow.value.appKey,
        reason: auditForm.reason
      })
      message.success('已拒绝（不建任何主体）')
      auditVisible.value = false
    }
    await getList()
  } finally {
    auditLoading.value = false
  }
}

// ========== 开通结果（初始密码一次性下发） ==========
const openingVisible = ref(false)
const openingResult = ref<ApplicationApi.ApplicationOpeningRespVO>()
const copyInitialPassword = async () => {
  if (!openingResult.value?.initialPassword) return
  await navigator.clipboard.writeText(openingResult.value.initialPassword)
  message.success('已复制初始密码')
}

onMounted(() => {
  getList()
})
</script>
