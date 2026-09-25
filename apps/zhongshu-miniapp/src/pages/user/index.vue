<template>
  <view class="yd-page-container">
    <!-- 顶部背景区域 -->
    <view class="header-bg h-120rpx w-full flex items-center justify-center" />

    <!-- 用户信息卡片 -->
    <view class="relative mx-24rpx -mt-60rpx">
      <view
        class="user-card flex items-center rounded-12rpx bg-white p-32rpx"
        @click="handleGoProfile"
      >
        <view class="avatar-wrapper mr-24rpx">
          <wd-img :src="userInfo.avatar" width="120rpx" height="120rpx" mode="aspectFill" round />
        </view>
        <view class="flex-1">
          <view class="mb-8rpx text-40rpx text-[#323333] font-semibold">
            {{ userInfo.nickname || userInfo.username }}
          </view>
          <view class="text-30rpx text-[#777]">
            {{ userProfile ? (userProfile.dept?.name || '暂无部门') : '' }}
          </view>
        </view>
      </view>
    </view>

    <!-- 菜单区域 -->
    <view class="mx-24rpx mt-32rpx">
      <wd-cell-group custom-class="menu-group" border>
        <TenantVisitPicker
          v-if="showVisitPicker"
          :options="visitOptions"
          @confirm="handleTenantConfirm"
        >
          <template #default="{ value }">
            <wd-cell title="当前租户" :value="value" is-link>
              <template #prefix>
                <wd-icon name="home" size="20px" color="#1677ff" class="mr-16rpx" />
              </template>
            </wd-cell>
          </template>
        </TenantVisitPicker>
        <wd-cell title="个人资料" is-link @click="handleGoProfile">
          <template #prefix>
            <wd-icon name="user" size="20px" color="#1890ff" class="mr-16rpx" />
          </template>
        </wd-cell>
        <wd-cell title="账号安全" is-link @click="handleGoSecurity">
          <template #prefix>
            <wd-icon name="lock" size="20px" color="#52c41a" class="mr-16rpx" />
          </template>
        </wd-cell>
      </wd-cell-group>
      <wd-cell-group custom-class="menu-group mt-24rpx" border>
        <wd-cell title="常见问题" is-link @click="handleGoFaq">
          <template #prefix>
            <wd-icon name="exclamation-circle" size="20px" color="#faad14" class="mr-16rpx" />
          </template>
        </wd-cell>
        <wd-cell title="意见反馈" is-link @click="handleGoFeedback">
          <template #prefix>
            <wd-icon name="edit" size="20px" color="#722ed1" class="mr-16rpx" />
          </template>
        </wd-cell>
        <wd-cell title="联系客服" is-link @click="handleGoContact">
          <template #prefix>
            <wd-icon name="phone" size="20px" color="#13c2c2" class="mr-16rpx" />
          </template>
        </wd-cell>
        <wd-cell title="应用设置" is-link @click="handleGoSettings">
          <template #prefix>
            <wd-icon name="settings" size="20px" color="#1890ff" class="mr-16rpx" />
          </template>
        </wd-cell>
      </wd-cell-group>
      <view class="mt-48rpx">
        <wd-button block type="danger" @click="handleLogout">
          退出登录
        </wd-button>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import type { UserProfileVO } from '@/api/system/user/profile'
import type { VisitSwitchIO, VisitTarget, VisitTenantOption } from '@/utils/tenant-visit'
import { useDialog } from '@wot-ui/ui/components/wd-dialog'
import { useToast } from '@wot-ui/ui/components/wd-toast'
import { storeToRefs } from 'pinia'
import { computed, onMounted, ref } from 'vue'
import { getMyVisitTargets } from '@/api/system/cross-org-visit'
import { getUserProfile } from '@/api/system/user/profile'
import { LOGIN_PAGE } from '@/router/config'
import { useUserStore } from '@/store'
import { useDictStore } from '@/store/dict'
import { useTokenStore } from '@/store/token'
import { buildVisitTenantOptions, performVisitSwitch, planVisitSwitch, resolveStaleVisit } from '@/utils/tenant-visit'
import TenantVisitPicker from './components/tenant-visit-picker.vue'

definePage({
  style: {
    navigationStyle: 'custom',
  },
})

const userStore = useUserStore()
const tokenStore = useTokenStore()
const dictStore = useDictStore()
const toast = useToast()
const dialog = useDialog()
const { userInfo } = storeToRefs(userStore)
const userProfile = ref<UserProfileVO | null>(null) // 用户详细信息
const tenantEnabled = computed(() => import.meta.env.VITE_APP_TENANT_ENABLE === 'true') // 租户开关
// ZS-SEC-001.A：跨租户访问能力总开关，默认关闭；关闭时不显示租户切换入口
const tenantVisitEnabled = computed(() => import.meta.env.VITE_APP_TENANT_VISIT_ENABLE === 'true')
// ZS-CLIENT-002.B：跨组织访问（获批业务组织导航）——服务端下单的授权目标为唯一数据源
const visitTargets = ref<VisitTarget[]>([]) // 服务端下发的授权目标
const visitOptions = ref<VisitTenantOption[]>([]) // 切换选项（登录租户恒为首项）
const visitLoginTenantId = ref<number | null>(null) // 登录租户编号（切换计划判定基准）
// 切换入口显隐：仅「获批（服务端返回非空目标列表）」才显示——不再依赖旧粗粒度 system:tenant:visit
const showVisitPicker = computed(() => tenantEnabled.value && tenantVisitEnabled.value && visitTargets.value.length > 0)

/** 页面加载时获取用户信息 */
onMounted(async () => {
  userProfile.value = await getUserProfile()
  await userStore.fetchUserInfo()
  await loadVisitTargets()
})

/** 跳转到个人资料 */
function handleGoProfile() {
  uni.navigateTo({ url: '/pages-core/user/profile/index' })
}

/** 跳转到账号安全 */
function handleGoSecurity() {
  uni.navigateTo({ url: '/pages-core/user/security/index' })
}

/** 跳转到常见问题 */
function handleGoFaq() {
  uni.navigateTo({ url: '/pages-core/user/faq/index' })
}

/** 跳转到意见反馈 */
function handleGoFeedback() {
  uni.navigateTo({ url: '/pages-core/user/feedback/index' })
}

/** 跳转联系客服 */
function handleGoContact() {
  uni.navigateTo({ url: '/pages-core/user/contact/index' })
}

/** 跳转到应用设置 */
function handleGoSettings() {
  uni.navigateTo({ url: '/pages-core/user/settings/index' })
}

/**
 * 加载跨组织访问授权目标（ZS-CLIENT-002.B：服务端为唯一真相源，客户端不做授权推导）。
 * 未获批 / 加载失败 → 清空目标（不显示切换入口）；失效访问态（授权撤销 / 过期 / 脏状态）→ 清理本地访问态。
 */
async function loadVisitTargets() {
  if (!tenantEnabled.value || !tenantVisitEnabled.value) {
    return
  }
  try {
    const res = await getMyVisitTargets()
    if (res.loginTenantId == null) {
      return
    }
    const currentVisitTenantId = userStore.visitTenantId != null ? Number(userStore.visitTenantId) : null
    const stale = resolveStaleVisit(currentVisitTenantId, res.loginTenantId, res.targets)
    if (stale) {
      // 仅清理本地脏状态，不打断当前浏览；下次导航 / 刷新即回到登录租户视野
      userStore.setVisitTenantId(null)
    }
    visitLoginTenantId.value = res.loginTenantId
    visitTargets.value = res.targets
    visitOptions.value = buildVisitTenantOptions(
      res.loginTenantId,
      res.loginTenantName ?? '',
      res.targets,
      stale ? null : currentVisitTenantId,
    )
  } catch {
    visitTargets.value = []
    visitOptions.value = []
  }
}

/** 切换当前访问的租户（获批业务组织导航）：noop 幂等跳过；执行切换并失败回滚，绝不遗留半切态 */
async function handleTenantConfirm(option: VisitTenantOption) {
  const loginTenantId = visitLoginTenantId.value
  if (loginTenantId == null) {
    return
  }
  const currentVisitTenantId = userStore.visitTenantId != null ? Number(userStore.visitTenantId) : null
  const plan = planVisitSwitch(option.tenantId, loginTenantId, currentVisitTenantId)
  if (plan.action === 'noop') {
    return
  }
  try {
    await dialog.confirm({
      title: '切换租户',
      msg: plan.action === 'restore'
        ? `确定恢复访问登录租户「${option.name}」吗？`
        : `确定切换至租户「${option.name}」吗？切换后业务数据将按该租户展示。`,
    })
  } catch {
    return
  }
  // 访问租户只切换数据上下文，用户与权限仍沿用登录租户；刷新权限失败（授权已撤销）自动回滚
  const io: VisitSwitchIO = {
    setVisitTenantId: id => userStore.setVisitTenantId(id),
    clearDictCache: () => dictStore.clearDictCache(),
    refreshUserInfo: async () => {
      await userStore.fetchUserInfo()
    },
    reLaunch: url => uni.reLaunch({ url }),
    toast: message => toast.error(message),
  }
  await performVisitSwitch(io, plan)
}

/** 退出登录 */
async function handleLogout() {
  try {
    await dialog.confirm({
      title: '提示',
      msg: '确定要退出登录吗？',
    })
  } catch {
    return
  }

  await tokenStore.logout()
  toast.success('退出登录成功')
  setTimeout(() => {
    uni.reLaunch({ url: LOGIN_PAGE })
  }, 500)
}
</script>

<style lang="scss" scoped>
// 顶部渐变背景
.header-bg {
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
}

// 用户卡片阴影
.user-card {
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.08);
}

// 头像边框
.avatar-wrapper {
  border: 4rpx solid #f5f5f5;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
}

// 菜单组样式
:deep(.menu-group) {
  border-radius: 12rpx;
  overflow: hidden;
  box-shadow: 0 3rpx 8rpx rgba(24, 144, 255, 0.06);
}
</style>
