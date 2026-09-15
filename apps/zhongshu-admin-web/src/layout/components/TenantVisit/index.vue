<template>
  <div>
    <el-select
      filterable
      placeholder="请选择租户"
      class="!w-180px"
      v-model="value"
      @change="handleChange"
      clearable
    >
      <el-option v-for="item in tenants" :key="item.id" :label="item.name" :value="item.id" />
    </el-select>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import * as TenantApi from '@/api/system/tenant'
import { getVisitTenantId, setVisitTenantId } from '@/utils/auth'
import { useMessage } from '@/hooks/web/useMessage'
import { useTagsView } from '@/hooks/web/useTagsView'
import { clearAuthorizedSession } from '@/utils/authSession'

const message = useMessage() // 消息弹窗
const tagsView = useTagsView() // 标签页操作

const value = ref(getVisitTenantId()) // 当前选中的租户 ID
const tenants = ref<any[]>([]) // 租户列表

const handleChange = (id: number) => {
  // 设置访问租户 ID
  setVisitTenantId(id)
  // ZS-CLIENT-001.A：技术租户变化必须清理旧路由 / 缓存 / 页签 / 数据（卡片「调整」）。
  // 原 `tagsView.closeOther()` 只关闭其他页签并**保留当前页**，旧租户的 keep-alive 实例、
  // 字典快照与已装配路由全部存活；改为统一清理合同（保留凭据与 VisitTenantId），
  // 随后 refreshPage 触发全局守卫重新 bootstrap，按新租户上下文重拉授权与菜单。
  clearAuthorizedSession('tenant-switch')
  // 刷新当前页面
  tagsView.refreshPage()
  // 提示切换成功
  const tenant = tenants.value.find((item) => item.id === id)
  if (tenant) {
    message.success(`切换当前租户为: ${tenant.name}`)
  }
}

onMounted(async () => {
  tenants.value = await TenantApi.getTenantList()
})
</script>
