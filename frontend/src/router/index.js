import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { applyTheme } from '@/utils/theme'

const DefaultLayout = () => import('@/layouts/DefaultLayout.vue')
const UserLayout = () => import('@/layouts/UserLayout.vue')
const AdminLayout = () => import('@/layouts/AdminLayout.vue')

const routes = [
  {
    path: '/',
    component: DefaultLayout,
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/portal/HomeView.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'articles',
        name: 'articles',
        component: () => import('@/views/portal/ArticleListView.vue'),
        meta: { title: '全部文章' }
      },
      {
        path: 'search',
        name: 'search',
        component: () => import('@/views/portal/ArticleListView.vue'),
        meta: { title: '搜索' }
      },
      {
        path: 'categories/:id',
        name: 'category',
        component: () => import('@/views/portal/ArticleListView.vue'),
        meta: { title: '分类' }
      },
      {
        path: 'tags/:id',
        name: 'tag',
        component: () => import('@/views/portal/ArticleListView.vue'),
        meta: { title: '标签' }
      },
      {
        path: 'articles/:id',
        name: 'article-detail',
        component: () => import('@/views/portal/ArticleDetailView.vue'),
        meta: { title: '文章详情' }
      },
      {
        path: 'login',
        name: 'login',
        component: () => import('@/views/auth/LoginView.vue'),
        meta: { title: '登录', guestOnly: true }
      },
      {
        path: 'register',
        name: 'register',
        component: () => import('@/views/auth/RegisterView.vue'),
        meta: { title: '注册', guestOnly: true }
      },
      {
        path: 'editor/:id?',
        name: 'editor',
        component: () => import('@/views/editor/EditorView.vue'),
        meta: { title: '写文章', requiresAuth: true }
      }
    ]
  },
  {
    path: '/',
    component: UserLayout,
    children: [
      {
        path: 'me',
        name: 'me',
        component: () => import('@/views/user/ProfileView.vue'),
        meta: { title: '个人资料', requiresAuth: true }
      },
      {
        path: 'my/articles',
        name: 'my-articles',
        component: () => import('@/views/user/MyArticlesView.vue'),
        meta: { title: '我的文章', requiresAuth: true }
      },
      {
        path: 'my/comments',
        name: 'my-comments',
        component: () => import('@/views/user/MyCommentsView.vue'),
        meta: { title: '我的评论', requiresAuth: true }
      },
      {
        path: 'my/collects',
        name: 'my-collects',
        component: () => import('@/views/user/MyCollectsView.vue'),
        meta: { title: '我的收藏', requiresAuth: true }
      }
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { theme: 'admin', requiresAuth: true, role: 'ADMIN' },
    children: [
      {
        path: '',
        name: 'admin-dashboard',
        component: () => import('@/views/admin/DashboardView.vue'),
        meta: { title: '数据总览' }
      },
      {
        path: 'articles',
        name: 'admin-articles',
        component: () => import('@/views/admin/AdminArticlesView.vue'),
        meta: { title: '文章管理与审核' }
      },
      {
        path: 'comments',
        name: 'admin-comments',
        component: () => import('@/views/admin/AdminCommentsView.vue'),
        meta: { title: '评论审核' }
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/AdminUsersView.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'taxonomy',
        name: 'admin-taxonomy',
        component: () => import('@/views/admin/AdminTaxonomyView.vue'),
        meta: { title: '分类与标签' }
      },
      {
        path: 'logs',
        name: 'admin-logs',
        component: () => import('@/views/admin/AdminLogsView.vue'),
        meta: { title: '审核与操作记录' }
      }
    ]
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/error/ForbiddenView.vue'),
    meta: { title: '没有权限' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, saved) {
    if (saved) return saved
    return { top: 0 }
  }
})

router.beforeEach(async (to) => {
  // 主题：后台走 Linear 深色，其余走 Claude 暖色。
  // 用 to.matched（命中的路由记录）而不是合并后的 to.meta，避免父级 meta 未合并带来的坑。
  const isAdminArea = to.matched.some((record) => record.meta && record.meta.theme === 'admin')
  applyTheme(isAdminArea)

  const store = useUserStore()
  // 只要还没确认过登录态就先问一次（公开页面同样需要，见 store 注释）
  if (!store.checked) {
    await store.fetchMe()
  }
  if (to.meta.requiresAuth && !store.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && store.isLoggedIn) {
    return { name: 'home' }
  }
  if (to.meta.role && store.user?.role !== to.meta.role) {
    return { name: 'forbidden' }
  }
  return true
})

router.afterEach((to) => {
  const base = '个人博客'
  document.title = to.meta.title ? `${to.meta.title} · ${base}` : base
})

export default router