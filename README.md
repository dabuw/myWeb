# 个人求职主页（Spring Boot + Vue + MySQL）

用于个人求职展示的全栈项目，支持：

- 公开展示页（个人资料 + 项目列表）
- 后台管理页（`/myself`）
- 管理密码校验（数据库 BCrypt + 后端接口级保护）
- 本地开发与 Docker 部署

## 1. 技术栈

- 后端：Spring Boot 3 + Spring Data JPA + MySQL
- 前端：Vue 3 + Vite
- 部署：Docker / Docker Compose

## 2. 主要目录

- `backend`：后端服务
- `frontend`：前端页面
- `docker-compose.yml`：本地一体化（含本地 MySQL）
- `docker-compose.cloud.yml`：云部署（连接外部 MySQL）
- `.env.example`：环境变量模板

## 3. 功能说明

### 3.1 公开接口

- `GET /api/public/portfolio`：获取个人资料与项目

### 3.2 管理接口（需要 `X-Admin-Password`）

- `GET /api/admin/auth-check`：校验管理密码
- `POST /api/admin/change-password`：修改管理密码
- `PUT /api/admin/profile`：修改个人资料（`profiles`）
- `POST /api/admin/projects`：新增项目（`project_links`）
- `PUT /api/admin/projects/{id}`：更新项目
- `DELETE /api/admin/projects/{id}`：删除项目

### 3.3 管理页面

- 路径：`/myself`
- 登录方式：输入管理密码
- 可操作内容：
	- 修改姓名、邮箱、职位、简介、GitHub、LinkedIn 等
	- 在线修改管理密码（无需重启后端）
	- 新增/编辑/删除项目
	- 调整 `displayOrder` 控制展示顺序

## 4. 环境变量

复制模板：

```bash
cp .env.example .env
```

Windows PowerShell：

```powershell
Copy-Item .env.example .env
```

重点变量：

- `SPRING_PROFILES_ACTIVE`：`local` 或 `cloud`
- `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD`
- `DB_USE_SSL`：云数据库通常按实际配置
- `CORS_ALLOWED_ORIGIN`：允许访问后端的前端地址；生产环境填写实际域名，例如 `https://dabuw.online`
- `VITE_API_BASE_URL`：前端打包时注入的后端 API 地址。生产环境应留空，前端 Nginx 会将同域 `/api` 转发到后端，避免跨域与 HTTPS 混合内容错误

管理员密码说明：

- 管理密码存储于数据库表 `admin_users`（字段 `password_hash`）
- 后端使用 BCrypt 校验与更新密码哈希
- 首次部署前请先在数据库初始化管理员账号

## 5. 本地开发（不使用 Docker）

### 5.1 启动后端

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="local"
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/portfolio_site?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="123456"
mvn spring-boot:run
```

### 5.2 启动前端

```powershell
cd frontend
npm install
npm run dev -- --host
```

访问：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- 后台：`http://localhost:5173/myself`

## 6. Docker 本地一体化（含容器 MySQL）

```bash
docker compose up -d --build
```

访问：

- 前端：`http://localhost:8081`
- 后端：`http://localhost:8080`

停止：

```bash
docker compose down
```

## 7. Docker 云部署（外部 MySQL）

1. 准备 `.env`（参考 `.env.example`，填写云数据库配置）
	- 默认前端端口为 `8082`（避免与常见 Redis 管理工具 `8081` 冲突）
	- 域名为 `dabuw.online` 且已配置 HTTPS 时，设置 `CORS_ALLOWED_ORIGIN=https://dabuw.online`
	- 设置 `VITE_API_BASE_URL=`（留空）。不要填写服务器 IP 或 `http://...:8080`
2. 启动：

```bash
docker compose -f docker-compose.cloud.yml --env-file .env up -d --build
```

3. 查看日志：

```bash
docker compose -f docker-compose.cloud.yml logs -f
```

4. 停止：

```bash
docker compose -f docker-compose.cloud.yml down
```

云部署访问：

- 前端：`http://<服务器IP>:8082`
- 后端：`http://<服务器IP>:8080`

### 7.1 域名部署

将服务器上的站点配置反向代理到前端容器端口（本例为 `8082`）。域名入口不需要、也不应直接暴露后端 `8080` 端口：

```nginx
server {
  listen 80;
  server_name dabuw.online www.dabuw.online;

  location / {
    proxy_pass http://127.0.0.1:8082;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
}
```

配置 HTTPS 后，将 `listen 80` 改为证书对应的 HTTPS 站点配置，并保持同样的 `proxy_pass`。容器中的前端 Nginx 会把浏览器访问的 `/api/...` 自动转发给后端。

## 8. 部署前检查清单

- `admin_users` 已存在管理员账号且密码哈希有效
- 云数据库已开放应用服务器 IP 白名单
- `CORS_ALLOWED_ORIGIN` 与实际前端域名一致
- `VITE_API_BASE_URL` 留空，确保 API 通过同域 `/api` 访问
- 本项目默认不再自动插入 `data.sql` 初始化数据
