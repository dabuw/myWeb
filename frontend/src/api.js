const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export async function fetchPortfolio() {
  const response = await fetch(`${API_BASE}/api/public/portfolio`);
  if (!response.ok) {
    throw new Error(`加载数据失败: ${response.status}`);
  }
  return response.json();
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
    throw new Error(`管理接口请求失败: ${response.status}`);
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
