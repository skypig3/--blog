import request from './request'

/** 分类与标签（公开读取）。 */
export const taxonomyApi = {
  categories: () => request.get('/categories'),
  tags: () => request.get('/tags')
}

/** 系统信息（公开）。 */
export const systemApi = {
  info: () => request.get('/system/info')
}

/** 个人资料与密码。 */
export const userApi = {
  profile: () => request.get('/user/profile'),
  updateProfile: (payload) => request.put('/user/profile', payload),
  changePassword: (payload) => request.put('/user/password', payload)
}

/** 图片上传。 */
export const fileApi = {
  upload: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/files/image', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}