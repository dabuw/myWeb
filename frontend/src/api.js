// In Docker deployments, Nginx proxies this path to the backend so requests
// stay on the same origin as the site.
const API_BASE = import.meta.env.VITE_API_BASE_URL || '';

export async function fetchPortfolio() {
  const response = await fetch(`${API_BASE}/api/public/portfolio`);
  if (!response.ok) {
    throw new Error(`加载数据失败: ${response.status}`);
  }
  return response.json();
}

/**
 * 从错误响应里取出后端给的提示语。
 * 后端在 401 / 429 等响应里会带 { message: "..." }（见 GlobalExceptionHandler），
 * 取到就用它，前端才能显示「登录尝试次数过多，请在 X 分钟后重试」这类具体原因。
 */
async function readErrorMessage(response) {
  try {
    const body = await response.clone().json();
    const message = body?.message;
    return typeof message === 'string' ? message.trim() : '';
  } catch {
    return '';
  }
}

async function adminFetch(path, password, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'X-Admin-Password': password,
      ...(options.headers || {})
    }
  });

  if (!response.ok) {
    const message = await readErrorMessage(response);
    throw new Error(message || `管理接口请求失败: ${response.status}`);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

export async function checkAdminPassword(password) {
  await adminFetch('/api/admin/auth-check', password, { method: 'GET' });
}

export async function updateAdminProfile(password, profile) {
  return adminFetch('/api/admin/profile', password, {
    method: 'PUT',
    body: JSON.stringify(profile)
  });
}

export async function createProject(password, project) {
  return adminFetch('/api/admin/projects', password, {
    method: 'POST',
    body: JSON.stringify(project)
  });
}

export async function updateProject(password, id, project) {
  return adminFetch(`/api/admin/projects/${id}`, password, {
    method: 'PUT',
    body: JSON.stringify(project)
  });
}

export async function deleteProject(password, id) {
  return adminFetch(`/api/admin/projects/${id}`, password, {
    method: 'DELETE'
  });
}

export async function changeAdminPassword(password, oldPassword, newPassword) {
  return adminFetch('/api/admin/change-password', password, {
    method: 'POST',
    body: JSON.stringify({
      oldPassword,
      newPassword
    })
  });
}
