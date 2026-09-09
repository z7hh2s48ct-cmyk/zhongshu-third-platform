/**
 * FAQ 常见问题数据
 */
export interface FaqItem {
  /** 问题标题 */
  title: string
  /** 问题答案 */
  content: string
}

export interface FaqCategory {
  /** 分类图标 */
  icon: string
  /** 分类标题 */
  title: string
  /** 问题列表 */
  childList: FaqItem[]
}

/** FAQ 数据列表 */
export const faqList: FaqCategory[] = [
  {
    icon: 'github-filled',
    title: '常见问题',
    childList: [
      {
        title: '众墅之家是什么平台？',
        content: '开源，基于 MIT 协议，可免费商用。',
      },
      {
        title: '众墅之家可以商用吗？',
        content: '可以，平台底座采用 MIT 开源协议，允许商业使用。',
      },
      {
        title: '众墅之家官网地址多少？',
        // hotfix-D P2-2：原答案把上游官网/文档域名呈现为众墅之家官网/文档，属产品可见界面引用上游链接，
        // 违反 docs/06 第 4.1 节（上游参考链接仅保留于注释/.http 示例/上游元数据）。正式域名就绪前用占位文案。
        content: '官网正在建设中，敬请期待。',
      },
      {
        title: '众墅之家文档地址多少？',
        content: '文档正在建设中，敬请期待。',
      },
    ],
  },
  {
    icon: 'exclamation-circle',
    title: '其他问题',
    childList: [
      {
        title: '如何退出登录？',
        content: '请点击 [我的] - [退出登录] 即可退出登录。',
      },
      {
        title: '如何修改用户头像？',
        content: '请点击 [我的] - [个人资料] - [选择头像] 即可更换用户头像。',
      },
      {
        title: '如何修改登录密码？',
        content: '请点击 [我的] - [账号安全] - [修改密码] 即可修改登录密码。',
      },
      {
        title: '如何切换用户？',
        content: '请先退出当前账号，然后使用其他账号重新登录即可。',
      },
    ],
  },
]
