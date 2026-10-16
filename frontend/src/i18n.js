// 中英文案字典 + 语言判定。
//
// 语言由 URL 前缀决定：/en 开头 = 英文，其余 = 中文。
// 容器里的 Nginx 用 `try_files $uri $uri/ /index.html` 兜底，所以 /en 会拿到
// 同一份 index.html，再由 Vue 按路径渲染对应语言——不需要 vue-router，
// 也不需要改 Nginx 配置。

export const messages = {
  zh: {
    htmlLang: 'zh-CN',
    documentTitle: '个人项目作品集',
    brand: '个人项目作品集',
    projects: '项目展示',
    projectsHint: '独立开发项目',
    featured: '重点项目',
    viewProject: '查看项目',
    codeRepo: '代码仓库',
    loading: '正在加载…',
    loadFailed: '数据加载失败，请稍后刷新重试',
    langSwitch: 'English',
    langSwitchTitle: 'Switch to English version',
  },
  en: {
    htmlLang: 'en',
    documentTitle: 'Personal Portfolio',
    brand: 'Personal Portfolio',
    projects: 'Projects',
    projectsHint: 'Independently Developed Projects',
    featured: 'Featured',
    viewProject: 'View project',
    codeRepo: 'Repository',
    loading: 'Loading…',
    loadFailed: 'Failed to load data. Please refresh and try again.',
    langSwitch: '中文',
    langSwitchTitle: '切换到中文版',
  },
};

/** 根据当前路径判断语言：/en 开头为英文，否则中文。 */
export function detectLang() {
  return window.location.pathname.toLowerCase().startsWith('/en') ? 'en' : 'zh';
}

/** 返回一个 t('key') 取词函数，缺失的键回退到中文，再回退到键名本身。 */
export function createTranslator(lang) {
  const table = messages[lang] || messages.zh;
  return (key) => table[key] ?? messages.zh[key] ?? key;
}

/**
 * 取出某个字段在当前语言下的文本。
 * 英文页优先取 `xxxEn`；该字段为空时回退到中文字段，保证页面不会出现空白。
 */
export function pickText(item, field, lang) {
  if (!item) {
    return '';
  }
  if (lang === 'en') {
    const translated = item[`${field}En`];
    if (typeof translated === 'string' && translated.trim()) {
      return translated;
    }
  }
  const value = item[field];
  return value == null ? '' : value;
}
