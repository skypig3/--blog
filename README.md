# 个人博客系统 · Personal Blog System

课程项目：Spring Boot + MyBatis-Plus + SQLite 的个人博客系统，含文章审核与评论审核。

> **当前完成状态（2026-10-09，前端已重做）**
> 前后端**都已完成并实测通过**。
> 后端：51 个接口、25 个自动化测试、102 项端到端检查全绿。
> **前端已按两套设计系统重做**：前台（门户 + 个人中心）用 `frontend/DESIGN.md` 的 Claude 暖色编辑风，
> 后台用 `backend/DESIGN.md` 的 Linear 深色产品风；两套主题共用一套自研的轻量组件库，
> 换主题只在 `<body>` 上切一个 class。`pnpm build` 通过（主包 326 kB / gzip 131 kB）。
> 重做后在浏览器里实测了：首页/列表/搜索/详情、登录（演示账号快捷填充）、发文存草稿、
> 文章审核通过与驳回、评论审核、分类标签维护、操作日志筛选、越权拦截（普通用户访问 `/admin` → `/403`）、
> 前台与后台主题互不串色。
> 页面截图（`docs/截图/`，13 张）与人工测试步骤文档（`docs/测试用例与人工测试步骤.md`）已补齐。
> 详见[已知限制](#已知限制)。

---

## 1. 项目简介

个人博客展示平台 + 注册用户投稿 + 管理员审核。游客只能浏览已发布内容，
注册用户能创作与互动，管理员能审核与维护。

主要功能：

| 模块 | 内容 |
|---|---|
| 用户 | 注册、登录（用户名或邮箱）、退出、改资料、改密码、账号禁用/恢复、角色变更 |
| 文章 | 草稿、提交审核、撤回、编辑、删除；Markdown 正文、封面、分类、多标签 |
| 审核 | 文章通过/驳回（驳回必填原因）、违规下架、状态机防重复审核 |
| 评论 | 顶层评论与回复、评论审核（覆盖回复）、删除 |
| 互动 | 点赞、收藏（幂等开关）、我的收藏列表 |
| 前台查询 | 分页列表、标题关键词搜索、分类筛选、标签筛选、热门文章、阅读量 |
| 后台 | 七项统计（实时查库）、近期文章与待办审核、审核与操作日志、分类标签维护 |

技术选型的过程和结论写在 [docs/设计文档.md](docs/设计文档.md)，
其中包含每个版本为什么这么选、以及实测证据。

---

## 2. 环境版本

下面这些是**本机实际跑出来的版本**，不是从教程抄的。

| 项 | 版本 | 怎么确认 |
|---|---|---|
| Java (JDK) | 21.0.10 LTS | `java -version` |
| Maven | 3.9.16 | `mvn -v`（本机装在 `D:\Dev\apache-maven-3.9.16`，**不在 PATH 里**，见常见问题） |
| Spring Boot | 4.1.1 | `backend/pom.xml` 的 parent |
| Spring Framework | 7.0.9 | `mvn dependency:tree` 实测 |
| MyBatis-Plus | 3.5.17（`mybatis-plus-spring-boot4-starter`） | 同上 |
| MyBatis / mybatis-spring | 3.5.19 / 4.0.0 | 同上 |
| JSqlParser | 5.2（`mybatis-plus-jsqlparser`，分页插件所在模块） | 同上 |
| SQLite JDBC | 3.53.4.0（内核 3.53.4） | 同上 |
| springdoc-openapi | 3.1.1（Swagger UI / OpenAPI 3.1） | 同上 |
| spring-security-crypto | 7.1.1（只用 BCrypt，不引入 Security 过滤器链） | 同上 |
| Node.js / npm | v24.18.0 / 11.16.0 | `node -v`、`npm -v` |
| **pnpm** | 11.15.1（**前端包管理器用 pnpm**，锁文件 `pnpm-lock.yaml`，lockfileVersion 9.0） | `pnpm -v` |
| Git | 2.55.0 | `git --version` |

前端依赖的实际安装版本（从 `frontend/node_modules/*/package.json` 读，不是从文档抄的）：

| 包 | 版本 | 用途 |
|---|---|---|
| vue | 3.5.43 | 框架 |
| vue-router | 4.6.4 | 路由与页面级权限守卫 |
| pinia | 2.3.1 | 登录态 |
| axios | 1.20.0 | 统一请求层 |
| markdown-it | 14.3.2 | Markdown 渲染 |
| dompurify | 3.4.16 | 渲染结果消毒（防 XSS） |
| vite / @vitejs/plugin-vue | 6.4.4 / 5.2.4 | 构建（要求 Node ≥ 20.19） |

前端**没有引入 Element Plus**：设计系统（衬线大标题 + 暖米画布 / 近黑阶梯）与通用组件库的默认外观
冲突较大，所以两套主题共用一套自研的轻量组件库（`src/components/ui/`，约 17 个基础组件）。
副作用是主包体积从「全量引入 Element Plus」的 870 kB / gzip 277 kB 降到
**326 kB / gzip 131 kB**；代价是表格、下拉、弹窗这些都要自己实现一遍。

> pnpm 11 默认不执行依赖的构建脚本，esbuild 装不出平台二进制会让 `vite build` 失败。
> 项目在 `frontend/pnpm-workspace.yaml` 里用 `allowBuilds` 放行了 `esbuild` 与 `vue-demi`，
> 所以直接 `pnpm install` 即可，不需要再手工 `pnpm approve-builds`。

---

## 3. 目录说明

```
personal-blog/
├─ backend/                    后端（Spring Boot + MyBatis-Plus + SQLite）
│  ├─ DESIGN.md                ★ 后台设计系统：Linear 深色产品风（近黑画布 + 薰衣草主色 + 表面阶梯）
│  ├─ pom.xml  mvnw  mvnw.cmd  Maven 构建文件与包装器
│  ├─ src/main/java/com/sky/blog/
│  │  ├─ BlogApplication.java        启动类（@MapperScan + 先建 data/ 目录）
│  │  ├─ common/                统一响应 ApiResult、错误码、全局异常、TimeUtil
│  │  ├─ config/                分页插件、Web 装配、BCrypt、OpenAPI、演示数据导入器
│  │  ├─ security/              ★ 登录鉴权与权限判断（AuthInterceptor / @RequireLogin / @RequireRole）
│  │  ├─ entity/ mapper/        9 张表的实体与 Mapper
│  │  ├─ service/               ★ 业务规则：状态机、归属校验、计数一致性、统计
│  │  ├─ controller/            接口层
│  │  ├─ dto/ vo/               入参与出参（VO 结构上不存在密码字段）
│  │  └─ enums/                 角色、账号状态、文章状态机、评论状态、日志类型
│  ├─ src/main/resources/
│  │  ├─ application.yml        端口、数据库路径、上传目录、热门规则权重
│  │  └─ db/                    schema.sql 建表 / seed.sql 初始账号 / demo-data.sql 演示数据
│  ├─ src/test/java/            25 个自动化测试
│  ├─ tools/e2e.sh              102 项端到端检查脚本
│  └─ data/                     运行时生成：blog.db 与 uploads/（已 gitignore）
├─ frontend/                   前端（Vue 3 + Vite + 自研轻量组件库，pnpm 管理）
│  ├─ DESIGN.md                ★ 前台设计系统：Claude 暖色编辑风（暖米画布 + 珊瑚主色 + 衬线大标题）
│  ├─ package.json  pnpm-lock.yaml  pnpm-workspace.yaml
│  ├─ vite.config.js           代理 /api 与 /uploads 到后端；端口可用环境变量覆盖
│  └─ src/
│     ├─ main.js  App.vue  router/index.js  stores/user.js
│     ├─ styles/               tokens.css（双主题设计令牌）base.css（复位+排版）admin.css（后台表格等）
│     ├─ components/ui/        自研基础组件库（全局注册，视图直接用）：
│     │                        Button/Input/Textarea/Select/Field/Card/Badge/Avatar/Modal/
│     │                        Pagination/Empty/Skeleton/Spinner/Dropdown/MenuItem/Icon/FeedbackHost/MarkdownView
│     ├─ components/portal/    AuthPanel / ArticleCard / ImageUploader
│     ├─ api/                  request.js（统一错误处理 + 401 跳登录）+ auth/articles/site/admin
│     ├─ composables/          feedback.js（消息提示/确认框）adminActions.js（后台审核动作封装）
│     ├─ layouts/              DefaultLayout（前台）UserLayout（个人中心）AdminLayout（后台）+ parts/ 头部页脚
│     ├─ utils/                markdown.js（渲染+消毒）format.js（状态/时间文案）theme.js（主题切换）
│     └─ views/                portal(3) auth(2) editor(1) user(4) admin(6) error(2)
├─ docs/                       设计文档、接口文档、截图、选型实测证据
│  └─ spike/                   技术选型阶段的实测工程与原始输出
└─ README.md                   本文件
```

### 3.1 双设计系统（前台 Claude / 后台 Linear）

前端有两套完全不同的视觉主题，分别来自两份设计规范，切换只靠 `<body>` 上的一个 class：

| | 前台（门户 + 个人中心） | 后台（管理台） |
|---|---|---|
| 规范 | [frontend/DESIGN.md](frontend/DESIGN.md) | [backend/DESIGN.md](backend/DESIGN.md) |
| 风格 | Claude 暖色编辑风 | Linear 深色产品风 |
| 画布 | 暖米 `#faf9f5` | 近黑 `#010102` |
| 主色 | 珊瑚 `#cc785c` | 薰衣草蓝 `#5e6ad2` |
| 标题字 | 衬线（Cormorant Garamond ≈ Copernicus） | 无衬线（Inter，600 字重） |
| 表面 | 浅米卡片 `#efe9de` + 深色页脚 | surface-1…4 阶梯 + 1px 发丝边 |

实现要点：颜色 / 字体 / 圆角 / 间距全部在 `src/styles/tokens.css` 里定义成**语义令牌**
（`--bg` / `--surface` / `--primary` / `--text` …），组件只消费语义令牌，
所以同一套组件在两套主题下外观自动切换，视图代码里没有任何主题判断。

**为什么用 `<body>` class 而不是 `<html data-theme>`**：实测发现某些宿主环境会往 `<html>` 注入自己的
`data-theme`（例如 `dark`），会把应用设置的值覆盖掉，后台页面于是被渲染成前台配色。
改用 `body.theme-portal` / `body.theme-admin` 后不再被干扰；`index.html` 里还有一段内联脚本按路径预先设置 class，
避免深链到 `/admin` 时首屏闪一下浅色。主题在路由守卫（按 `to.matched` 判断）与 `AdminLayout` 挂载时各设置一次。

---

## 4. 配置方法

全部配置集中在 `backend/src/main/resources/application.yml`，**没有隐藏配置**。

| 要改的东西 | 配置项 | 默认值 |
|---|---|---|
| 后端端口 | `server.port` | `8080` |
| 数据库文件路径 | `spring.datasource.url` | `jdbc:sqlite:./data/blog.db`（相对**启动目录**，即在 `backend/` 下启动就是 `backend/data/blog.db`） |
| 上传目录 | `blog.upload-dir` | `./data/uploads` |
| 上传大小/类型 | `blog.upload-max-size-mb`、`blog.upload-allowed-ext` | `2`、`jpg,jpeg,png,gif,webp` |
| 热门规则 | `blog.hot-window-days`、`blog.hot-limit`、`blog.hot-weight-*` | 30 天、10 篇、1/2/3/1 |
| SQL 日志 | `mybatis-plus.configuration.log-impl` | 默认打到控制台；想安静改成 `org.apache.ibatis.logging.nologging.NoLoggingImpl` |

**前端接口地址**：默认走 Vite 代理，`/api` 与 `/uploads` 都转发到 `http://localhost:8080`，
同源所以 Session Cookie 天然能带，不需要处理跨域。
后端端口不是 8080 时用环境变量覆盖，不必改代码：

```bash
VITE_API_TARGET=http://localhost:8081 pnpm dev
```

（后端也已经放行了 `http://localhost:*` / `http://127.0.0.1:*` 的跨域请求并允许携带凭据，
所以前端直连后端端口也能跑，但推荐用代理。）

**数据库路径不随启动目录变化**：`blog.data-dir` 由 `BlogApplication` 在启动前算成绝对路径，
固定落在 `backend/data`。
这条是实测踩出来的：命令行 `java -jar` 的工作目录是 `backend/`，
而 IntelliJ 的运行配置默认工作目录是**项目根**，
用相对路径 `./data` 时两边会各自打开一个不同的 `blog.db`，
表现就是"明明导入过演示数据，页面却是空的"，两个进程还互相看不见对方的写入。

配置示例（`application-local.yml.example`）：如需覆盖端口和数据库路径，
在 `backend/src/main/resources/` 下新建 `application-local.yml` 并写：

```yaml
server:
  port: 8081
spring:
  datasource:
    url: "jdbc:sqlite:D:/tmp/blog-test.db?foreign_keys=on&busy_timeout=5000&journal_mode=WAL"
```

启动时加 `--spring.profiles.active=local` 生效（该文件已在 `.gitignore` 里）。

---

## 5. 数据初始化

分三层，**互不覆盖**：

| 脚本 | 何时执行 | 会覆盖数据吗 |
|---|---|---|
| `db/schema.sql` | 每次启动自动执行 | 不会。全部 `CREATE TABLE IF NOT EXISTS` |
| `db/seed.sql` | 每次启动自动执行 | 不会。全部 `INSERT OR IGNORE`，只保证 admin/xiaoming/xiaohong 三个账号和 4 分类 6 标签存在 |
| `db/demo-data.sql` | **只在你手动执行导入命令时** | **会！开头一组 DELETE 会清空全部业务数据** |

### 建表 + 初始账号（自动，无需操作）

后端第一次启动就会自己建好 `data/` 目录、建表、写入三个演示账号。

### 导入演示数据（可选，⚠ 会清空现有数据）

```bash
cd backend
java -jar target/blog-backend-1.0.0.jar --blog.load-demo-data=true
```

导入内容：12 篇文章（7 已发布 / 3 待审核 / 2 被驳回）、9 条评论（含回复与待审核、驳回各若干）、
8 条点赞、5 条收藏、6 条操作日志。时间用 `datetime('now','-N day')` 生成，
所以任何时候导入，"最近 30 天"的热门窗口内都有内容。

导入完会打印一组自检数字，其中三行 `*_mismatch=0` 用来确认
文章上的冗余计数与关系表行数一致（后台统计的可信度依赖这个）。

### 重置成全新库

停掉服务，删掉 `backend/data/blog.db`（连同 `-wal`、`-shm`），再启动即可。
SQLite 就是单文件，删掉等于重建。

---

## 6. 启动命令

以下命令都在 `backend/` 目录下执行。**先确认 JAVA_HOME 指向 JDK 21**（见常见问题第 1 条）。

```bash
# Windows PowerShell / cmd
set JAVA_HOME=D:\Dev\JDK21
mvnw.cmd spring-boot:run

# Git Bash
export JAVA_HOME="D:/Dev/JDK21"
./mvnw spring-boot:run
```

启动成功的标志：控制台出现 `Started BlogApplication in x.x seconds`，
然后浏览器打开 <http://localhost:8080/swagger-ui.html>。

前端（另开一个终端）：

```bash
cd D:\学习项目\personal-blog\frontend
pnpm install          # 首次；之后按 pnpm-lock.yaml 还原
pnpm dev              # → http://127.0.0.1:5173
```

页面入口见[第 8 节](#8-访问入口)。前端和后端要**同时**起着才能看到数据。

---

## 7. 构建方法

```bash
cd backend
set JAVA_HOME=D:\Dev\JDK21

mvnw -B clean test            # 跑 25 个自动化测试（用独立的 target/test-blog.db，不碰演示库）
mvnw -B -DskipTests package   # 打包 → target/blog-backend-1.0.0.jar
```

用构建结果运行：

```bash
java -jar target/blog-backend-1.0.0.jar
```

前端构建：

```bash
cd frontend
pnpm build            # → frontend/dist/
```

要单端口部署，把 `frontend/dist/` 的内容拷进 `backend/src/main/resources/static/`
（或运行期的静态目录）再重新打包，这样前台、个人中心、后台都由 8080 一个端口提供。
当前开发阶段是"后端 8080 + 前端 5173 代理"两个进程，功能完全一致。

---

## 8. 访问入口

后端已可用（服务启动后）：

| 入口 | 地址 |
|---|---|
| **接口文档 Swagger UI** | <http://localhost:8080/swagger-ui.html>（自动跳转到 `/swagger-ui/index.html`） |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |
| 服务自检 | <http://localhost:8080/api/system/info> |

Swagger UI 上可以直接试接口：先 `POST /api/auth/login` 登录，
浏览器会拿到 `JSESSIONID` Cookie，之后同一页面上的受保护接口自动带上它，不用手动填 token。

前端页面（`pnpm dev` 后，基址 <http://127.0.0.1:5173>）：

| 入口 | 地址 | 权限 |
|---|---|---|
| 博客首页 | <http://127.0.0.1:5173/> | 公开 |
| 全部文章 / 搜索 | `/articles`、`/search?q=SQLite` | 公开 |
| 分类 / 标签 | `/categories/1`、`/tags/3` | 公开 |
| 文章详情（含评论、点赞收藏） | `/articles/101` | 公开可读 |
| 登录 / 注册 | `/login`、`/register` | 公开 |
| 写文章 / 编辑 | `/editor`、`/editor/116` | 需登录 |
| 个人中心 | `/me`、`/my/articles`、`/my/comments`、`/my/collects` | 需登录 |
| **后台管理** | <http://127.0.0.1:5173/admin> | 需 ADMIN |

---

## 9. 演示账号

> 仅用于本地测试。密码哈希是真实 BCrypt 生成后写死在 `db/seed.sql` 里的，
> 不要把这套凭据用到任何公网环境。

| 账号 | 密码 | 角色 | 用途 |
|---|---|---|---|
| `admin` | `Admin@123` | ADMIN | 后台审核、用户管理、统计 |
| `xiaoming` | `User@123` | USER | 作者侧：草稿、提交、撤回、评论、点赞收藏 |
| `xiaohong` | `User@123` | USER | 第二个普通用户，用来验证归属校验和互动计数 |

---

## 10. 测试方法

### 自动化测试

```bash
cd backend && ./mvnw test
```

25 个用例，跑在**独立的** `target/test-blog.db` 上，不会污染演示数据；每个用例带事务回滚。
覆盖：状态机全部流转、重复审核被拒、撤回后审核被拒、驳回必填原因、
归属校验、最后一个管理员保护、评论与回复的审核链路、点赞收藏幂等、
统计与 SQL 对账、BCrypt 与改密后会话失效、时间格式与 SQLite 默认值一致。

### 端到端接口检查

```bash
cd backend
./mvnw -B -DskipTests package
java -jar target/blog-backend-1.0.0.jar --blog.load-demo-data=true   # 先灌演示数据
java -jar target/blog-backend-1.0.0.jar &                            # 再起服务
bash tools/e2e.sh                                                    # 102 项检查
```

结果写在 `backend/tools/.e2e/result.txt`，退出码就是失败项数。
脚本里所有断言都用 `OK` / `FAIL` 标注，含中文的请求体一律走 UTF-8 文件（避免代码页问题）。

### 浏览器实测（已完成的路径）

前后端同时启动后，在浏览器里真点过这些路径。**前端重做之后**又按同样方式复测了一遍，
两批结果都是用无障碍树快照 + 页面内脚本（读计算样式与 DOM）拿到的（截不到图，见[已知限制](#已知限制)）。

**重做版前端（当前代码）的复测结果：**

| 检查 | 结果 |
|---|---|
| 前台主题 = Claude 暖色 | 通过：`body` class `theme-portal`，背景 `rgb(250,249,245)`，主按钮 `rgb(204,120,92)` |
| 首页渲染 | 通过：Hero 衬线大标题 + 深色代码卡片 + 6 张最新文章卡 + 热门 7 条 + 分类/标签计数 + 深色页脚 |
| 列表页搜索与筛选 | 通过：搜索「Vue」后地址变 `/search?q=Vue`，结果只剩 1 篇；分类/标签/排序下拉可用 |
| 文章详情 | 通过：Markdown 渲染出 h2 与代码块，评论区两层结构，点赞/收藏计数正确 |
| 登录页演示账号快捷填充 | 通过：点「管理员」自动填 admin/Admin@123，登录 toast「登录成功」、顶栏变用户菜单；刷新保持登录态 |
| 后台主题 = Linear 深色 | 通过：`body` class `theme-admin`，背景 `rgb(1,1,2)`，侧栏 `rgb(15,16,17)`，主按钮 `rgb(94,106,210)` |
| 后台仪表盘七项统计 | 通过：七个数字全部实时来自库，与接口一致 |
| 文章 / 评论审核 | 通过：点「通过」→ 确认 → toast + 列表刷新 + 侧栏待审角标 -1 |
| 分类与标签维护 | 通过：分类 4 / 标签 6 正确渲染，新建与删除标签生效（顺带修掉了「标签列表接口缺失」的 bug） |
| 操作日志 | 通过：能看到操作者/动作/目标/结果/说明，按动作筛选生效 |
| 编辑器存草稿 | 通过：toast「草稿已保存」，地址由 `/editor` 变为 `/editor/120`，标题变「编辑文章」，预览渲染 Markdown 正常 |
| 越权拦截 | 通过：普通用户访问 `/admin` 被守卫重定向到 `/403`，标题「没有权限 · 个人博客」；后端同样 403 |
| 主题互不串色 | 通过：后台 → 前台来回切换后，前台仍是暖米色、后台仍是近黑色 |
| 控制台报错 | 无（修复后新开标签页复测，控制台为空） |

**第一版前端当时的复测记录**（同一套后端接口，过程记在 `docs/spike/B2-frontend-checks.txt`）：

| 检查 | 结果 |
|---|---|
| 首页渲染：最新 5 篇 + 分页 2 页 + 热门 7 篇 + 分类/标签计数 | 通过（热门顺序 320/210/150/90/60/40/25 与权重一致） |
| 登录页"演示账号"快捷填充 + 提交 | 通过，toast「登录成功」，跳回首页，顶栏变用户菜单 |
| 后台仪表盘七个数字 vs 演示数据 | 完全一致（3/12/7/3/9/2/895） |
| 文章管理与审核：点「通过」→ 确认 | 通过，待审核列表从 3 条变 2 条 |
| 文章详情 Markdown 渲染 | 通过（`## 配置` 渲染成 h2，代码块内容正确） |
| 详情页点赞 | 通过，计数 0 → 1，toast「已记录」 |
| 发表评论 | 通过，进入待审核；「我的评论」显示"待审核"，已通过的另一条显示"已通过" |
| 编辑器存草稿 | 通过，创建 id=116，地址替换为 `/editor/116`，状态标签"草稿" |
| 普通用户访问 `/admin` | 被路由守卫重定向到 `/403`，标题变「没有权限」 |
| 刷新页面后保持登录态 | 修复后通过（见下一节） |

### 人工测试步骤

见 [docs/测试用例与人工测试步骤.md](docs/测试用例与人工测试步骤.md)。
按角色分成游客 / 注册用户 / 管理员 / 主题横切四组，共 45 条用例，每条含操作与预期结果，
末尾附记录模板与失败判据。

---

## 11. 常见问题

**1. 编译出来的字节码不是 21，或者报版本相关的怪错**
本机 `JAVA_HOME` 指向 `D:\Dev\jdk26`（JDK 26），而 PATH 上的 `java` 是 21。
Maven 认 `JAVA_HOME`，所以不覆盖就会用 JDK 26 编译。启动前显式指回 21：

```bat
set JAVA_HOME=D:\Dev\JDK21
```

确认方法：`mvnw -v` 输出里 `runtime:` 那一行应该是 `D:\Dev\JDK21`。

**2. `mvn` 命令找不到**
本机 Maven 装在 `D:\Dev\apache-maven-3.9.16` 但没进 PATH。
用项目自带的 `mvnw` / `mvnw.cmd` 即可（首次会联网下载 Maven 发行包）；
或者直接把 `D:\Dev\apache-maven-3.9.16\bin` 加进 PATH。

**3. 端口 8080 被占用**
`.\mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`，
或改 `application.yml` 的 `server.port`。
查占用：`netstat -ano | findstr :8080`（比 PowerShell 的 `Get-NetTCPConnection` 快得多）。

**4. 启动报 `Failed to execute database script` / `[SQLITE_CANTOPEN]`**
真因几乎都是**数据库文件的父目录不存在**——SQLite 不会自动建目录。
正常启动时 `BlogApplication` 会先建 `data/`，所以这个错通常是：
你改了 `spring.datasource.url` 指到一个不存在的目录，或者从别的目录启动导致相对路径对不上。
解决：手动建好目录，或把 URL 改成绝对路径。

**5. 控制台中文报错是乱码**
Maven / javac 的中文诊断信息按 GBK 写出，Git Bash 里看就是乱码。
判断构建成败请看退出码和 `BUILD SUCCESS` 这类 ASCII 字样；
非要读中文就 `... | iconv -f GBK -t UTF-8`。
注意：`MAVEN_OPTS="-Dstderr.encoding=UTF-8"` 实测**修不了**这个，别浪费时间。

**6. 重启服务后要重新登录**
Session 存在服务进程内存里，重启即失效，这是设计如此（见设计文档 §3.4）。

**7. 报 `SQLITE_BUSY: database file is locked`**
SQLite 是单写者模型。连接池已设为 1 并带 `busy_timeout=5000`，
正常不会遇到。若遇到，多半是**同时开了两个后端进程**指向同一个库文件
（比如忘了关上一次启动的进程），先确认 8080 上只有一个进程。

**8. 演示数据导入后统计数字和接口不一致**
看导入命令输出的三行 `*_mismatch=0`。不为 0 说明计数被手工改过，
重新导入一次演示数据即可。

---

## 12. 已知限制

如实列出未完成与已知缺陷：

**未完成**

1. `docs/接口文档.md` 待单独整理（目前接口说明以 Swagger 为准，
   参数与错误码约定写在 `docs/设计文档.md` §5.1）。
2. Git 仓库的提交记录还没整理（作业 1.5 要求"保留项目开发记录"）。
3. 单端口部署（把 `frontend/dist/` 交给后端托管）命令写好了但**没实测过**，
   目前验证的是"后端 8080 + 前端 5173 代理"这种双进程形态。

**已完成（本轮补齐）**

- 页面截图已补齐：`docs/截图/` 下 13 张（前台首页/列表/详情/编辑器/个人中心/我的文章/登录，
  后台仪表盘/文章/评论/用户/分类标签/日志），由内置浏览器实拍。
  取证方式也从早先的"无障碍树快照 + 页面内脚本"升级为直接截图。
- 人工测试步骤文档已补齐：`docs/测试用例与人工测试步骤.md`。

**设计与实现上的取舍（不是 bug，但答辩要能讲清）**

1. **Session 存在内存里**：重启服务需要重新登录。没做数据库级会话持久化。
2. **改密码只保证"当前会话立即失效 + 其它会话靠版本号在下一次请求被挡"**，
   不是"一键踢掉所有设备"那种即时推送。
3. **已发布文章被作者编辑后立刻退出公开状态**（回到草稿或待审核）。
   依据是作业 1.4"未经审核的内容不得公开"。代价：改一个错别字也要重新过审。
4. **关键词搜索只搜标题**，用 `LIKE '%kw%'`，不走全文索引。
   数据量大时会慢，但作业明确把"全文搜索引擎"排除在必做范围外。
5. **中文排序按 UTF-8 字节序，不是拼音序**（SQLite 没有 ICU 排序规则），
   所以不提供"按标题排序"，列表一律按时间/热度/id 排。
6. **用户名是逻辑删除，唯一约束仍然占用**：删号后该用户名不能被重新注册。
7. **CSRF 防护靠同源部署 + `SameSite=Lax` Cookie**，没有加 CSRF token。
    生产环境需要补，课程项目按同源交付处理。
8. **热门排序权重（阅读 1 / 点赞 2 / 收藏 3 / 评论 1）是设计选择，不是算法结论**，
    没有做同任务同数据的对比实验。
9. **图片只校验扩展名 + 大小 + 文件头魔数**，不做病毒扫描，也不做 EXIF 清理。
10. 评论只支持两级（顶层 + 回复），不支持回复一条回复。
11. 前端 UI 是**自研的轻量组件库**（`src/components/ui/`，约 17 个基础组件），没有用 Element Plus——
    两套设计系统（衬线编辑风 / 近黑产品风）与通用组件库默认外观冲突较大，自己写反而更省事。
    主包因此是 326 kB / gzip 131 kB（对比旧版全量引入 Element Plus 的 870 kB / gzip 277 kB）。
    代价：表格、下拉、弹窗、分页都要自己实现与维护。
12. 前台没有做暗色模式（后台界面本身就是深色主题，但那是两套独立设计，不是前台的 dark mode）。
13. 前端表单的校验规则和后端 `AuthDto` / `ArticleDto` 上的注解是**手工保持一致**的两份，
    改一边忘了改另一边就会出现"前端放过、后端 400"。后端校验始终才是权威。
14. 后台「操作记录」里的**操作者显示的是用户名快照**（如 `admin`），不是昵称（`站长`）——
    这是后端 `AuditLogService` 写入 `operator_name` 时的设计（用户改名/删号后历史日志仍可读），
    前端如实展示接口返回值，没有做额外的昵称映射。
15. 本地 `backend/data/blog.db` 是**长期沿用的演示库**，历次联调会产生新数据
    （草稿文章、待审评论、被点过的赞与阅读量），所以后台统计可能与 README 记录的初始演示数字不同。
    需要干净状态时：停掉服务 → 删掉 `backend/data/blog.db`（连同 `-wal`/`-shm`）→ 重新启动，
    或再执行一次演示数据导入（注意：演示导入会清空全部业务数据）。

---

## 13. 关键逻辑位置速查（对应作业 6.2）

| 作业要求 | 代码位置 |
|---|---|
| 登录鉴权 | `backend/src/main/java/com/sky/blog/security/AuthInterceptor.java`（每请求回库校验状态与凭据版本） |
| 权限判断（角色） | 同上 + `@RequireRole` / `@RequireLogin` 注解，注册在 `config/WebMvcConfig.java` |
| 数据归属校验 | `service/ArticleService.java` 的 `assertOwned()`；`service/CommentService.java` 的 `delete()` |
| 文章审核 | `service/ArticleService.java` 的 `audit()` / `offline()`，状态流转表在 `enums/ArticleStatus.java` |
| 评论审核 | `service/CommentService.java` 的 `audit()`，可审核状态集合在 `enums/CommentStatus.java` |
| 防重复审核 | 上述两个 `audit()` 里的 `UPDATE ... WHERE status = 原状态`（CAS），影响 0 行即 409 |
| 密码不落明文 | `config/PasswordConfig.java`（BCrypt）+ `vo/UserVo.java`（出参结构里没有密码字段） |
| 统计来自真实库 | `service/StatsService.java`（全部 COUNT/SUM）+ `mapper/ArticleMapper.sumViewCount()` |
| 操作日志 | `service/AuditLogService.java` + `entity/AuditLog.java`（含操作者与目标标题快照） |
| 分页方言 | `config/MybatisPlusConfig.java`（`DbType.SQLITE` → 实测走 PostgreDialect 的 `LIMIT ? OFFSET ?`） |
