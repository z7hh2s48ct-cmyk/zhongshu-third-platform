<template>
  <view class="yd-page-container yd-page-container-paging">
    <wd-navbar title="线索工作台" left-arrow placeholder safe-area-inset-top fixed @click-left="handleBack" />

    <!-- 状态过滤（三视角范围由服务端解析） -->
    <wd-tabs v-model="statusTab" @change="handleTabChange">
      <wd-tab v-for="t in statusTabs" :key="t.key ?? 'all'" :title="t.title" />
    </wd-tabs>

    <z-paging
      ref="pagingRef"
      v-model="list"
      :fixed="false"
      class="min-h-0 flex-1"
      :default-page-size="10"
      :refresher-enabled="true"
      :inside-more="true"
      :loading-more-default-as-loading="true"
      empty-view-text="暂无线索"
      @query="queryList"
    >
      <view class="p-24rpx">
        <view
          v-for="item in list"
          :key="item.id"
          class="mb-20rpx rounded-16rpx bg-white p-24rpx"
          @click="handleDetail(item)"
        >
          <view class="mb-12rpx flex items-center justify-between">
            <text class="text-30rpx font-bold">{{ item.customerName || '未命名客户' }}</text>
            <wd-tag :type="statusTagType(item.status)" custom-class="!leading-20px">
              {{ statusLabel(item.status) }}
            </wd-tag>
          </view>
          <view class="mb-8rpx text-26rpx text-gray-500">
            编号：{{ item.leadKey }}
          </view>
          <view class="mb-8rpx text-26rpx text-gray-500">
            手机：{{ item.customerPhone || '-' }}
          </view>
          <view class="text-26rpx text-gray-500">
            员工：{{ item.assigneeUserId ?? '未分配' }}
          </view>
        </view>
      </view>
    </z-paging>
  </view>
</template>

<script lang="ts" setup>
import type { Lead } from '@/api/firstchain/lead'
import { getLeadPage, LEAD_STATUS } from '@/api/firstchain/lead'
import { navigateBackPlus } from '@/utils'

definePage({
  style: {
    navigationBarTitleText: '',
    navigationStyle: 'custom',
  },
})

const statusTabs: { key: string | undefined, title: string }[] = [
  { key: undefined, title: '全部' },
  { key: LEAD_STATUS.DISTRIBUTED, title: '已下发' },
  { key: LEAD_STATUS.ASSIGNED, title: '已分配' },
  { key: LEAD_STATUS.FOLLOWING, title: '跟进中' },
  { key: LEAD_STATUS.CONVERTED, title: '已转商机' },
  { key: LEAD_STATUS.INVALID, title: '已无效' },
]

const list = ref<Lead[]>([])
const pagingRef = ref<any>()
const statusTab = ref(0)

function statusLabel(status: string) {
  return statusTabs.find(t => t.key === status)?.title ?? status
}
function statusTagType(status: string): 'primary' | 'success' | 'danger' | 'warning' | 'default' {
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

/** z-paging 回调：pageNo/pageSize 由组件传入；视角范围由服务端解析 */
async function queryList(pageNo: number, pageSize: number) {
  try {
    const data = await getLeadPage({ status: statusTabs[statusTab.value]?.key, pageNo, pageSize })
    pagingRef.value?.completeByTotal(data.list, data.total)
  } catch {
    pagingRef.value?.complete(false)
  }
}

function handleTabChange() {
  pagingRef.value?.reload()
}

function handleDetail(item: Lead) {
  uni.navigateTo({ url: `/pages-firstchain/lead/detail/index?id=${item.id}` })
}

function handleBack() {
  navigateBackPlus()
}
</script>
