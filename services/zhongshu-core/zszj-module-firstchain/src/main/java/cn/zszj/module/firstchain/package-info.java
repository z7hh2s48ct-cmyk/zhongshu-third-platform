/**
 * firstchain 模块（首链业务模块：加盟商开通—线索闭环）。
 *
 * 立卡依据：docs/05 §15.3 ZS-FC-001/002/003（B09 排期新卡，D-07 M1~M10 已拍板）；
 * 接入遵循 services/zhongshu-core/docs/《新业务模块接入合同》12 项条款；
 * 领域状态机与幂等写回复用 zszj-module-bpm 的 cn.zszj.module.bpm.firstchain（ZS-BPM-003）。
 *
 * 一期范围（M9 排除项不得落地）：报价、合同、回款、客户 360、线索导入导出、
 * 超时自动回收、平台撤回已下发线索。
 */
package cn.zszj.module.firstchain;
