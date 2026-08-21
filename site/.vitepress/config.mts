import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'CRPI-FakePlayer',
  description: 'Fabric + Carpet 假人行为驱动模组 / Fake Player Behavior Driver Mod',
  lang: 'zh-CN',
  cleanUrls: true,

  head: [
    ['link', { rel: 'icon', type: 'image/svg+xml', href: '/logo.svg' }],
  ],

  locales: {
    root: {
      label: '中文',
      lang: 'zh-CN',
      themeConfig: {
        nav: [
          { text: '指南', link: '/guide/getting-started' },
          { text: 'API 参考', link: '/api/overview' },
          { text: '配置', link: '/config' },
          { text: '更新日志', link: '/changelog' },
        ],
        sidebar: {
          '/guide/': [
            {
              text: '指南',
              items: [
                { text: '快速开始', link: '/guide/getting-started' },
                { text: '安装部署', link: '/guide/installation' },
                { text: '命令参考', link: '/guide/commands' },
              ],
            },
          ],
          '/api/': [
            {
              text: 'API 参考',
              items: [
                { text: '概览', link: '/api/overview' },
                { text: 'Action API', link: '/api/actions' },
                { text: 'Control API', link: '/api/control' },
                { text: 'Navigation API', link: '/api/navigation' },
                { text: 'ActionPipeline', link: '/api/pipeline' },
              ],
            },
          ],
          '/config': [
            {
              text: '配置',
              items: [
                { text: '配置规则', link: '/config' },
              ],
            },
          ],
        },
      },
    },
    en: {
      label: 'English',
      lang: 'en-US',
      title: 'CRPI-FakePlayer',
      description: 'Fake Player Behavior Driver Mod for Fabric + Carpet',
      themeConfig: {
        nav: [
          { text: 'Guide', link: '/en/guide/getting-started' },
          { text: 'API Reference', link: '/en/api/overview' },
          { text: 'Config', link: '/en/config' },
          { text: 'Changelog', link: '/en/changelog' },
        ],
        sidebar: {
          '/en/guide/': [
            {
              text: 'Guide',
              items: [
                { text: 'Getting Started', link: '/en/guide/getting-started' },
                { text: 'Installation', link: '/en/guide/installation' },
                { text: 'Commands', link: '/en/guide/commands' },
              ],
            },
          ],
          '/en/api/': [
            {
              text: 'API Reference',
              items: [
                { text: 'Overview', link: '/en/api/overview' },
                { text: 'Action API', link: '/en/api/actions' },
                { text: 'Control API', link: '/en/api/control' },
                { text: 'Navigation API', link: '/en/api/navigation' },
                { text: 'ActionPipeline', link: '/en/api/pipeline' },
              ],
            },
          ],
          '/en/config': [
            {
              text: 'Config',
              items: [
                { text: 'Configuration Rules', link: '/en/config' },
              ],
            },
          ],
        },
      },
    },
  },

  themeConfig: {
    logo: '/logo.svg',
    siteTitle: 'CRPI-FakePlayer',
    socialLinks: [
      { icon: 'github', link: 'https://github.com/HeCUana/CRPI-FakePlayer' },
    ],
    search: {
      provider: 'local',
    },
    editLink: {
      pattern: 'https://github.com/HeCUana/CRPI-FakePlayer/edit/main/site/:path',
    },
    footer: {
      message: 'Released under the MIT License.',
      copyright: 'Copyright © 2024-2026 CRPI Team',
    },
  },
})
