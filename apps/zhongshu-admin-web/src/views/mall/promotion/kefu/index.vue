<template>
  <el-container class="kefu-layout">
    <!-- 会话列表 -->
    <KeFuConversationList ref="keFuConversationRef" @change="handleChange" />
    <!-- 会话详情（选中会话的消息列表） -->
    <KeFuMessageList ref="keFuChatBoxRef" />
    <!-- 会员信息（选中会话的会员信息） -->
    <MemberInfo ref="memberInfoRef" />
  </el-container>
</template>

<script lang="ts" setup>
import { KeFuConversationList, KeFuMessageList, MemberInfo } from './components'
import { WebSocketMessageTypeConstants } from './components/tools/constants'
import { KeFuConversationRespVO } from '@/api/mall/promotion/kefu/conversation'
import { getWsHandshakeTicket } from '@/api/login'
import { useWebSocket } from '@vueuse/core'
import { useMallKefuStore } from '@/store/modules/mall/kefu'

defineOptions({ name: 'KeFu' })

const message = useMessage() // 消息弹窗
const kefuStore = useMallKefuStore() // 客服缓存

// ======================= WebSocket start =======================
const server = ref('') // WebSocket 服务地址（开启连接时以一次性短时票据动态拼装，ZS-LOGIN-001.B）

/** 发起 WebSocket 连接（票据为一次性语义，autoReconnect 复用旧地址会被拒，须换票重连） */
const { status, ws, data, close, open } = useWebSocket(server, {
  immediate: false, // r0 P2-7：显式建连，防空 URL 初始连接
  autoConnect: false, // r0 P2-7：URL 变化不自动建连（否则同票据双建连、双消费）
  autoReconnect: false, // 票据一次性语义：重连须换票（下方退避链）
  heartbeat: true
})

// r0 P2-6：断线/取票失败统一走退避换票重连
let wsReconnectTimer: ReturnType<typeof setTimeout> | null = null
let wsReconnectAttempts = 0
let wsDisposed = false
const scheduleKefuWsReconnect = () => {
  if (wsDisposed || wsReconnectTimer) return
  // r2 P1：连接中判定须按实例实际 readyState——VueUse 14.3.0 普通断线后保留旧 ws 引用
  //（!=null 会误拦）、心跳超时清空 ws 但 status 停留 OPEN（===OPEN 会误拦），两种断开形态都要放行
  if (ws.value != null && ws.value.readyState === WebSocket.OPEN) return
  const delay = Math.min(1000 * 2 ** Math.min(wsReconnectAttempts, 5), 30000)
  wsReconnectAttempts++
  wsReconnectTimer = setTimeout(() => {
    wsReconnectTimer = null
    connectWithTicket()
  }, delay)
}
// r1 P2-5：VueUse 心跳超时路径会清空 ws 实例但 status 可能停留 OPEN——
// 以 ws 句柄清空为准触发换票重连（status=CLOSED 的常规路径同样覆盖）
watch(ws, (instance) => {
  if (instance == null && !wsDisposed) {
    scheduleKefuWsReconnect()
  }
})
watch(status, (s) => {
  if (s === 'OPEN') wsReconnectAttempts = 0
  if (s === 'CLOSED') scheduleKefuWsReconnect()
})

/** 取票并建连：POST /system/auth/ws-ticket（登录态）换一次性票据 → ?ticket= 握手 */
const connectWithTicket = async () => {
  let ticket: string
  try {
    ticket = await getWsHandshakeTicket()
  } catch (error) {
    console.error(error)
    scheduleKefuWsReconnect() // r0 P2-6：取票失败也走退避换票重连
    return
  }
  // r2 P2：取票等待期间页面可能已卸载（wsDisposed）——晚到票据不得建连（检查必须先于 open()）
  if (wsDisposed) return
  // r3 P2：隔离旧实例回调——旧连接迟到的 close 会无条件清掉 VueUse 共享心跳定时器
  //（新连接已被停心跳，半开连接无法自愈，codex 真实 TCP 复现）；旧实例即将销毁，解绑无副作用
  const previousWs = ws.value
  server.value =
    (import.meta.env.VITE_BASE_URL + '/infra/ws').replace('http', 'ws') +
    '?ticket=' +
    encodeURIComponent(ticket)
  open()
  if (previousWs && previousWs !== ws.value) {
    previousWs.onclose = null
    previousWs.onerror = null
    previousWs.onmessage = null
    previousWs.onopen = null
  }
}

/** 监听 WebSocket 数据 */
watch(
  () => data.value,
  (newData) => {
    if (!newData) return
    try {
      // 1. 收到心跳
      if (newData === 'pong') return

      // 2.1 解析 type 消息类型
      const jsonMessage = JSON.parse(newData)
      const type = jsonMessage.type
      if (!type) {
        message.error('未知的消息类型：' + newData)
        return
      }

      // 2.2 消息类型：KEFU_MESSAGE_TYPE
      if (type === WebSocketMessageTypeConstants.KEFU_MESSAGE_TYPE) {
        const message = JSON.parse(jsonMessage.content)
        // 刷新会话列表
        kefuStore.updateConversation(message.conversationId)
        // 刷新消息列表
        keFuChatBoxRef.value?.refreshMessageList(message)
        return
      }

      // 2.3 消息类型：KEFU_MESSAGE_ADMIN_READ
      if (type === WebSocketMessageTypeConstants.KEFU_MESSAGE_ADMIN_READ) {
        // 更新会话已读
        const message = JSON.parse(jsonMessage.content)
        kefuStore.updateConversationStatus(message.conversationId)
      }
    } catch (error) {
      console.error(error)
    }
  },
  {
    immediate: false // 不立即执行
  }
)
// ======================= WebSocket end =======================

/** 加载指定会话的消息列表 */
const keFuChatBoxRef = ref<InstanceType<typeof KeFuMessageList>>()
const memberInfoRef = ref<InstanceType<typeof MemberInfo>>()
const handleChange = (conversation: KeFuConversationRespVO) => {
  keFuChatBoxRef.value?.getNewMessageList(conversation)
  memberInfoRef.value?.initHistory(conversation)
}

const keFuConversationRef = ref<InstanceType<typeof KeFuConversationList>>()
/** 初始化 */
onMounted(() => {
  /** 加载会话列表 */
  kefuStore.setConversationList().then(() => {
    keFuConversationRef.value?.calculationLastMessageTime()
  })
  // 打开 websocket 连接（先取一次性票据）
  connectWithTicket()
})

/** 销毁 */
onBeforeUnmount(() => {
  wsDisposed = true // r0 P2-6：卸载后不再退避重连
  if (wsReconnectTimer) {
    clearTimeout(wsReconnectTimer)
    wsReconnectTimer = null
  }
  // 关闭 websocket 连接
  close()
})
</script>

<style lang="scss">
.kefu-layout {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  flex: 1;
}

/* 定义滚动条样式 */
::-webkit-scrollbar {
  width: 10px;
  height: 6px;
}

/* 定义滚动条轨道 内阴影+圆角 */
::-webkit-scrollbar-track {
  background-color: #fff;
  border-radius: 10px;
  box-shadow: inset 0 0 0 rgb(240 240 240 / 50%);
}

/* 定义滑块 内阴影+圆角 */
::-webkit-scrollbar-thumb {
  background-color: rgb(240 240 240 / 50%);
  border-radius: 10px;
  box-shadow: inset 0 0 0 rgb(240 240 240 / 50%);
}
</style>
