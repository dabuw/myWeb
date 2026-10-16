<script setup>
import { onMounted, ref } from 'vue';
import {
  changeAdminPassword,
  checkAdminPassword,
  createProject,
  deleteProject,
  fetchPortfolio,
  updateAdminProfile,
  updateProject
} from './api';
import { createTranslator, detectLang, pickText } from './i18n';

const lang = detectLang();
const t = createTranslator(lang);

/** 取当前语言下的字段文本；英文页缺字段时回退到中文，避免出现空白 */
function text(item, field) {
  return pickText(item, field, lang);
}

const loading = ref(true);
const error = ref('');
const profile = ref(null);
const projects = ref([]);
const isAdminPage = window.location.pathname === '/myself';

const authLoading = ref(false);
const adminPassword = ref(sessionStorage.getItem('admin_password') || '');
const authError = ref('');
const isAuthenticated = ref(false);
const actionMessage = ref('');
const actionError = ref('');
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const profileForm = ref({
  fullName: '',
  title: '',
  summary: '',
  location: '',
  fullNameEn: '',
  titleEn: '',
  summaryEn: '',
  locationEn: '',
  email: '',
  githubUrl: '',
  linkedinUrl: ''
});

const projectForm = ref({
  name: '',
  url: '',
  description: '',
  nameEn: '',
  descriptionEn: '',
  highlight: false,
  displayOrder: 0
});
const editingProjectId = ref(null);

function fillProfileForm(data) {
  profileForm.value = {
    fullName: data?.fullName || '',
    title: data?.title || '',
    summary: data?.summary || '',
    location: data?.location || '',
    fullNameEn: data?.fullNameEn || '',
    titleEn: data?.titleEn || '',
    summaryEn: data?.summaryEn || '',
    locationEn: data?.locationEn || '',
    email: data?.email || '',
    githubUrl: data?.githubUrl || '',
    linkedinUrl: data?.linkedinUrl || ''
  };
}

function resetProjectForm() {
  editingProjectId.value = null;
  projectForm.value = {
    name: '',
    url: '',
    description: '',
    nameEn: '',
    descriptionEn: '',
    highlight: false,
    displayOrder: 0
  };
}

async function loadPortfolioData() {
  const data = await fetchPortfolio();
  profile.value = data.profile;
  projects.value = data.projects || [];
  fillProfileForm(data.profile);
}

async function tryAutoAuth() {
  if (!adminPassword.value) {
    return;
  }
  try {
    await checkAdminPassword(adminPassword.value);
    isAuthenticated.value = true;
  } catch {
    sessionStorage.removeItem('admin_password');
    adminPassword.value = '';
  }
}

async function handleLogin() {
  authLoading.value = true;
  authError.value = '';
  try {
    await checkAdminPassword(adminPassword.value);
    sessionStorage.setItem('admin_password', adminPassword.value);
    isAuthenticated.value = true;
  } catch {
    authError.value = '密码错误，请重试';
    isAuthenticated.value = false;
  } finally {
    authLoading.value = false;
  }
}

function handleLogout() {
  sessionStorage.removeItem('admin_password');
  adminPassword.value = '';
  isAuthenticated.value = false;
  actionMessage.value = '';
  actionError.value = '';
}

async function handlePasswordChange() {
  actionMessage.value = '';
  actionError.value = '';

  if (passwordForm.value.newPassword.length < 8) {
    actionError.value = '新密码长度至少为 8 位';
    return;
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    actionError.value = '两次输入的新密码不一致';
    return;
  }

  try {
    await changeAdminPassword(
      adminPassword.value,
      passwordForm.value.oldPassword,
      passwordForm.value.newPassword
    );

    adminPassword.value = passwordForm.value.newPassword;
    sessionStorage.setItem('admin_password', passwordForm.value.newPassword);
    passwordForm.value = {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    };
    actionMessage.value = '管理密码已更新，并已自动切换为新密码会话';
  } catch (e) {
    actionError.value = e.message;
  }
}

async function handleProfileSave() {
  actionMessage.value = '';
  actionError.value = '';
  try {
    const updated = await updateAdminProfile(adminPassword.value, profileForm.value);
    profile.value = updated;
    fillProfileForm(updated);
    actionMessage.value = '个人资料已更新';
  } catch (e) {
    actionError.value = e.message;
  }
}

function startEditProject(item) {
  editingProjectId.value = item.id;
  projectForm.value = {
    name: item.name,
    url: item.url,
    description: item.description,
    nameEn: item.nameEn || '',
    descriptionEn: item.descriptionEn || '',
    highlight: !!item.highlight,
    displayOrder: item.displayOrder ?? 0
  };
}

async function handleProjectSubmit() {
  actionMessage.value = '';
  actionError.value = '';
  try {
    if (editingProjectId.value) {
      await updateProject(adminPassword.value, editingProjectId.value, projectForm.value);
      actionMessage.value = '项目已更新';
    } else {
      await createProject(adminPassword.value, projectForm.value);
      actionMessage.value = '项目已新增';
    }
    await loadPortfolioData();
    resetProjectForm();
  } catch (e) {
    actionError.value = e.message;
  }
}

async function handleProjectDelete(id) {
  actionMessage.value = '';
  actionError.value = '';
  try {
    await deleteProject(adminPassword.value, id);
    await loadPortfolioData();
    actionMessage.value = '项目已删除';
    if (editingProjectId.value === id) {
      resetProjectForm();
    }
  } catch (e) {
    actionError.value = e.message;
  }
}

onMounted(async () => {
  // 让 <html lang> 和标题跟随语言，英文页才算「真正的英文页面」
  document.documentElement.lang = t('htmlLang');
  document.title = t('documentTitle');

  try {
    await loadPortfolioData();
    if (isAdminPage) {
      await tryAutoAuth();
    }
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="background-glow"></div>

  <nav class="lang-switch" v-if="!isAdminPage">
    <a :href="lang === 'en' ? '/' : '/en'" :title="t('langSwitchTitle')">{{ t('langSwitch') }}</a>
  </nav>

  <main class="page" v-if="!isAdminPage">
    <section class="hero card" v-if="!loading && profile">
      <p class="tag">{{ t('brand') }}</p>
      <h1>{{ text(profile, 'fullName') }}</h1>
      <h2>{{ text(profile, 'title') }}</h2>
      <p class="summary">{{ text(profile, 'summary') }}</p>
      <div class="meta">
        <span v-if="text(profile, 'location')">{{ text(profile, 'location') }}</span>
        <a v-if="profile.email" :href="`mailto:${profile.email}`">{{ profile.email }}</a>
        <a v-if="profile.githubUrl" :href="profile.githubUrl" target="_blank" rel="noreferrer">GitHub</a>
        <a v-if="profile.linkedinUrl" :href="profile.linkedinUrl" target="_blank" rel="noreferrer">LinkedIn</a>
      </div>
    </section>

    <section class="card" v-if="!loading && projects.length">
      <div class="section-header">
        <h3>{{ t('projects') }}</h3>
        <p>{{ t('projectsHint') }}</p>
      </div>
      <ul class="project-grid">
        <li class="project-item" v-for="item in projects" :key="item.id">
          <div class="project-title-wrap">
            <h4>{{ text(item, 'name') }}</h4>
            <span v-if="item.highlight" class="badge">{{ t('featured') }}</span>
          </div>
          <p>{{ text(item, 'description') }}</p>
          <a :href="item.url" target="_blank" rel="noreferrer">{{ t('viewProject') }}</a>
        </li>
      </ul>
    </section>

    <section class="card state-card" v-if="loading">
      {{ t('loading') }}
    </section>

    <section class="card state-card error" v-if="!loading && error">
      {{ t('loadFailed') }}
    </section>
  </main>

  <main class="page" v-else>
    <section class="card" v-if="loading">
      正在加载管理页面...
    </section>

    <section class="card state-card error" v-else-if="error">
      {{ error }}
    </section>

    <template v-else>
      <section class="card" v-if="!isAuthenticated">
        <p class="tag">后台管理入口</p>
        <h3>请输入管理密码</h3>
        <div class="admin-form-row">
          <input
            class="input"
            type="password"
            v-model="adminPassword"
            placeholder="管理密码"
            @keyup.enter="handleLogin"
          />
          <button class="btn" :disabled="authLoading" @click="handleLogin">
            {{ authLoading ? '验证中...' : '进入管理台' }}
          </button>
        </div>
        <p v-if="authError" class="error-text">{{ authError }}</p>
      </section>

      <template v-else>
        <section class="card">
          <div class="admin-head">
            <div>
              <p class="tag">后台管理</p>
              <h3>个人资料与项目管理</h3>
            </div>
            <div class="admin-head-actions">
              <a class="btn btn-ghost" href="/">返回主界面</a>
              <button class="btn btn-ghost" @click="handleLogout">退出登录</button>
            </div>
          </div>
          <p v-if="actionMessage" class="success-text">{{ actionMessage }}</p>
          <p v-if="actionError" class="error-text">{{ actionError }}</p>
        </section>

        <section class="card">
          <h3>修改个人资料（profiles）</h3>
          <div class="form-grid">
            <input class="input" v-model="profileForm.fullName" placeholder="姓名（中文版）" />
            <input class="input" v-model="profileForm.title" placeholder="职位标题（中文版）" />
            <input class="input" v-model="profileForm.location" placeholder="所在城市（中文版）" />
            <input class="input" v-model="profileForm.email" placeholder="邮箱" />
            <input class="input" v-model="profileForm.githubUrl" placeholder="GitHub 链接" />
            <input class="input" v-model="profileForm.linkedinUrl" placeholder="LinkedIn 链接" />
            <textarea class="input textarea" v-model="profileForm.summary" placeholder="个人简介（中文版）"></textarea>
          </div>

          <p class="form-section-label">English version（留空则英文页回退显示中文）</p>
          <div class="form-grid">
            <input class="input" v-model="profileForm.fullNameEn" placeholder="Full name (EN)" />
            <input class="input" v-model="profileForm.titleEn" placeholder="Job title (EN)" />
            <input class="input" v-model="profileForm.locationEn" placeholder="Location (EN)" />
            <textarea class="input textarea" v-model="profileForm.summaryEn" placeholder="Summary (EN)"></textarea>
          </div>
          <button class="btn" @click="handleProfileSave">保存资料</button>
        </section>

        <section class="card">
          <h3>修改管理密码</h3>
          <div class="form-grid">
            <input
              class="input"
              type="password"
              v-model="passwordForm.oldPassword"
              placeholder="旧密码"
            />
            <input
              class="input"
              type="password"
              v-model="passwordForm.newPassword"
              placeholder="新密码（至少 8 位）"
            />
            <input
              class="input"
              type="password"
              v-model="passwordForm.confirmPassword"
              placeholder="确认新密码"
            />
          </div>
          <button class="btn" @click="handlePasswordChange">更新密码</button>
        </section>

        <section class="card">
          <h3>{{ editingProjectId ? '编辑项目' : '新增项目' }}（project_links）</h3>
          <div class="form-grid">
            <input class="input" v-model="projectForm.name" placeholder="项目名称" />
            <input class="input" v-model="projectForm.url" placeholder="项目链接" />
            <input class="input" type="number" v-model.number="projectForm.displayOrder" placeholder="排序" />
            <label class="checkbox-wrap">
              <input type="checkbox" v-model="projectForm.highlight" />
              <span>重点项目</span>
            </label>
            <textarea class="input textarea" v-model="projectForm.description" placeholder="项目描述（中文版）"></textarea>
          </div>

          <p class="form-section-label">English version（留空则英文页回退显示中文）</p>
          <div class="form-grid">
            <input class="input" v-model="projectForm.nameEn" placeholder="Project name (EN)" />
            <textarea class="input textarea" v-model="projectForm.descriptionEn" placeholder="Project description (EN)"></textarea>
          </div>
          <div class="actions">
            <button class="btn" @click="handleProjectSubmit">{{ editingProjectId ? '保存修改' : '添加项目' }}</button>
            <button v-if="editingProjectId" class="btn btn-ghost" @click="resetProjectForm">取消编辑</button>
          </div>
        </section>

        <section class="card">
          <h3>项目列表</h3>
          <ul class="project-grid" v-if="projects.length">
            <li class="project-item" v-for="item in projects" :key="item.id">
              <div class="project-title-wrap">
                <h4>{{ item.name }}</h4>
                <span v-if="item.highlight" class="badge">重点项目</span>
              </div>
              <p>{{ item.description }}</p>
              <a :href="item.url" target="_blank" rel="noreferrer">{{ item.url }}</a>
              <div class="actions">
                <button class="btn btn-ghost" @click="startEditProject(item)">编辑</button>
                <button class="btn btn-danger" @click="handleProjectDelete(item.id)">删除</button>
              </div>
            </li>
          </ul>
          <p v-else>暂无项目，先新增一个吧。</p>
        </section>
      </template>
    </template>
  </main>
</template>
