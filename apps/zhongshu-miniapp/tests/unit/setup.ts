import { vi } from 'vitest'

/**
 * ZS-CLIENT-003：移动端单元测试全局 setup。
 *
 * uni-app 运行时（全局 `uni`）在 node 测试环境不存在，这里提供最小可用桩：
 * - 存储 API：以内存 Map 模拟，供 token store 等按需读写
 * - 交互/网络 API：以 vi.fn() 占位，具体用例可再覆盖 uni.request 的行为
 * 被测模块通过 vi.mock 切断对真实 store / utils 的依赖，故此桩仅兜底。
 */
const storage = new Map<string, any>()

const uniMock: Record<string, any> = {
  getStorageSync: (key: string) => (storage.has(key) ? storage.get(key) : ''),
  setStorageSync: (key: string, value: any) => void storage.set(key, value),
  removeStorageSync: (key: string) => void storage.delete(key),
  clearStorageSync: () => storage.clear(),
  addInterceptor: vi.fn(),
  removeInterceptor: vi.fn(),
  showToast: vi.fn(),
  hideToast: vi.fn(),
  hideLoading: vi.fn(),
  showLoading: vi.fn(),
  request: vi.fn(),
  uploadFile: vi.fn(),
  downloadFile: vi.fn(),
  $emit: vi.fn(),
  $on: vi.fn(),
  $off: vi.fn(),
  navigateTo: vi.fn(),
  redirectTo: vi.fn(),
  reLaunch: vi.fn(),
  switchTab: vi.fn(),
  getAccountInfoSync: () => ({ miniProgram: { envVersion: 'release' } }),
}

;(globalThis as any).uni = uniMock

/** 供用例重置内存存储 */
export function __resetUniStorage() {
  storage.clear()
}
