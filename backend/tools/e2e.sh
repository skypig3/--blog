#!/usr/bin/env bash
# ============================================================================
#  端到端自测（后端）：覆盖作业 2.2 / 2.3 / 6.2 的每条硬要求
#
#  用法：cd backend && bash tools/e2e.sh
#  前提：服务已在 http://localhost:8080 运行，且已导入演示数据
#  结果：tools/.e2e/result.txt      退出码 = 失败项数
#
#  本脚本会改动数据（建文章、禁账号、改密码），
#  跑完请重新导入演示数据：
#     java -jar target/blog-backend-1.0.0.jar --blog.load-demo-data=true
#
#  两个环境坑（都实测过）：
#   1. 中文不能写在 curl 的 -d 里：Git Bash 传出去会被 ANSI 代码页吃掉，服务端收到的是坏 JSON。
#      所以所有含中文的请求体一律先用 python 写成 UTF-8 文件，再 --data-binary @file。
#   2. python 往管道打印中文默认按 cp936 编码，断言比较会失败，统一加 -X utf8。
# ============================================================================
set -u
cd "$(dirname "$0")/.." || exit 1          # 切到 backend/
BASE=http://localhost:8080
W=tools/.e2e
rm -rf "$W"; mkdir -p "$W"
OUT=$W/result.txt
JA=$W/admin.jar JB=$W/ming.jar JC=$W/hong.jar JC2=$W/hong2.jar
PY="python -X utf8"

n=0; fail=0
log() { n=$((n+1)); printf '%2d. %-44s %s\n' "$n" "$1" "$2" | tee -a "$OUT"; }
want() { # want 描述 实际值 期望子串
  local desc=$1 actual=$2 expect=$3
  if [[ "$actual" == *"$expect"* ]]; then log "$desc" "OK    $actual"
  else log "$desc" "FAIL  $actual   [期望包含 $expect]"; fail=$((fail+1)); fi
}
req() { # req METHOD PATH JARFILE [BODY]
  local m=$1 p=$2 jar=$3 data=${4:-}
  local args=(-s -o "$W/body.json" -w "%{http_code}" -X "$m" "$BASE$p" -H 'Content-Type: application/json')
  if [ -n "$jar" ]; then args+=(-b "$jar" -c "$jar"); else args+=(-b "$W/anon.jar" -c "$W/anon.jar"); fi
  [ -n "$data" ] && args+=(--data-binary "$data")
  curl "${args[@]}"
}
jqv() { $PY -c "
import json,sys
try:
    d=json.load(open('$W/body.json',encoding='utf-8')); print(eval(sys.argv[1]))
except Exception as e:
    print('ERR:'+type(e).__name__)
" "$1" 2>/dev/null; }
# 直接查库，用来跟接口返回对账（作业 2.3.1：统计必须来自真实数据库）
sql() { $PY -c "
import sqlite3,sys
c=sqlite3.connect('data/blog.db'); print(c.execute(sys.argv[1]).fetchone()[0]); c.close()
" "$1" 2>/dev/null; }
# body 文件名 python字典字面量  -> 写出 UTF-8 JSON 文件，返回 @路径
body() {
  $PY -c "
import json,sys
json.dump(eval(sys.argv[2]), open('$W/'+sys.argv[1],'w',encoding='utf-8'), ensure_ascii=False)
" "$1" "$2"
  printf '@%s/%s' "$W" "$1"
}

echo "=== 端到端自测 $(date '+%F %T') ===" | tee "$OUT"

# ---------------------------------------------------------------- 0 三个账号登录
c=$(req POST /api/auth/login "$JA" '{"account":"admin","password":"Admin@123"}')
want "管理员登录" "HTTP $c role=$(jqv "d['data']['role']")" "role=ADMIN"
c=$(req POST /api/auth/login "$JB" '{"account":"xiaoming","password":"User@123"}')
want "小明登录" "HTTP $c user=$(jqv "d['data']['username']")" "user=xiaoming"
c=$(req POST /api/auth/login "$JC" '{"account":"xiaohong","password":"User@123"}')
want "小红登录" "HTTP $c user=$(jqv "d['data']['username']")" "user=xiaohong"

# ---------------------------------------------------------------- 1 鉴权语义
c=$(req GET /api/auth/me "")
want "游客取 /auth/me 应 401" "HTTP $c code=$(jqv "d['code']")" "401 code=40100"
c=$(req POST /api/auth/login "" '{"account":"admin","password":"WRONG"}')
want "错密码应 401" "HTTP $c" "401"
c=$(req POST /api/auth/login "" '{"account":"nobody","password":"whatever"}')
want "用户不存在与密码错提示一致（防枚举）" "msg=$(jqv "d['message']")" "用户名或密码错误"

# ---------------------------------------------------------------- 2 注册规则
c=$(req POST /api/auth/register "" '{"username":"tester01","email":"t1@example.com","password":"Test1234","nickname":"tester"}')
want "注册新用户" "HTTP $c role=$(jqv "d['data']['role']")" "role=USER"
c=$(req POST /api/auth/register "" '{"username":"tester01","email":"other@example.com","password":"Test1234","nickname":"dup"}')
want "重复用户名应 409" "HTTP $c" "409"
c=$(req POST /api/auth/register "" '{"username":"tester01","email":"t1@example.com","password":"Test1234","nickname":"dup"}')
want "重复邮箱应 409" "HTTP $c" "409"
c=$(req POST /api/auth/register "" '{"username":"bad user!!","email":"not-an-email","password":"1","nickname":""}')
want "非法入参应 400" "HTTP $c" "400"
c=$(req POST /api/auth/register "" '{"username":"evil01","email":"evil@example.com","password":"Test1234","nickname":"e","role":"ADMIN"}')
want "注册时塞 role 字段也必须是 USER" "role=$(jqv "d['data']['role']")" "USER"
c=$(req GET "/api/admin/users?keyword=evil01" "$JA")
want "用户接口不返回密码哈希" "hasHash=$(jqv "'password' in json.dumps(d).lower()")" "hasHash=False"

# ---------------------------------------------------------------- 3 公开读取
c=$(req GET /api/categories "")
want "分类列表含真实计数" "HTTP $c $(jqv "d['data'][0]['name']+'='+str(d['data'][0]['articleCount'])") sql=$(sql "SELECT COUNT(*) FROM t_article WHERE category_id=1 AND status='PUBLISHED' AND deleted=0")" "技术笔记=3 sql=3"
c=$(req GET /api/tags "")
want "标签列表" "HTTP $c n=$(jqv "len(d['data'])")" "n=6"
c=$(req GET "/api/articles?current=1&size=5" "")
want "分页第1页" "HTTP $c total=$(jqv "d['data']['total']") n=$(jqv "len(d['data']['records'])") pages=$(jqv "d['data']['pages']")" "total=7 n=5 pages=2"
c=$(req GET "/api/articles?current=2&size=5" "")
want "分页第2页只剩2条" "HTTP $c n=$(jqv "len(d['data']['records'])")" "n=2"
c=$(req GET "/api/articles?keyword=SQLite" "")
want "英文关键词搜标题" "HTTP $c total=$(jqv "d['data']['total']") sql=$(sql "SELECT COUNT(*) FROM t_article WHERE title LIKE '%SQLite%' AND status='PUBLISHED' AND deleted=0")" "total=1 sql=1"
c=$(req GET "/api/articles?keyword=sqlite" "")
want "英文关键词大小写不敏感" "HTTP $c total=$(jqv "d['data']['total']")" "total=1"
c=$(req GET "/api/articles?keyword=%E5%93%8D%E5%BA%94%E5%BC%8F" "")
want "中文关键词能搜到" "HTTP $c total=$(jqv "d['data']['total']")" "total=1"
c=$(req GET "/api/articles?categoryId=1" "")
want "分类筛选" "HTTP $c total=$(jqv "d['data']['total']")" "total=3"
c=$(req GET "/api/articles?tagId=3" "")
want "标签筛选" "HTTP $c total=$(jqv "d['data']['total']")" "total=2"
c=$(req GET "/api/articles?keyword=zzz-nothing" "")
want "无结果时 total=0（前端据此显示空态）" "HTTP $c total=$(jqv "d['data']['total']")" "total=0"
c=$(req GET /api/articles/hot "")
want "热门按加权分排序" "order=$(jqv "[x['id'] for x in d['data']][:3]")" "order=[101, 102, 103"
c=$(req GET /api/articles/101 "")
want "详情（游客）" "HTTP $c status=$(jqv "d['data']['status']")" "status=PUBLISHED"
c=$(req GET /api/articles/108 "")
want "待审核文章对游客 404" "HTTP $c" "404"
c=$(req GET /api/articles/101/comments "")
want "评论树只含已通过" "top=$(jqv "sorted(x['id'] for x in d['data'])") childrenOf202=$(jqv "[x['id'] for p in d['data'] if p['id']==202 for x in p['children']]")" "top=[201, 202] childrenOf202=[203]"

# ---------------------------------------------------------------- 4 创作与审核闭环
c=$(req POST /api/articles "$JB" "$(body draft.json "{'title':'E2E 状态机测试文章','summary':'','contentMd':'## 正文\n\n脚本创建，验证草稿到发布。','categoryId':1,'tagIds':[2,3],'submit':False}")")
AID=$(jqv "d['data']['id']")
want "小明存草稿" "HTTP $c status=$(jqv "d['data']['status']")" "status=DRAFT"
c=$(req GET "/api/articles/$AID" "")
want "草稿对游客 404" "HTTP $c" "404"
c=$(req POST "/api/articles/$AID/submit" "$JB")
want "提交审核 DRAFT->PENDING" "HTTP $c status=$(jqv "d['data']['status']")" "status=PENDING"
c=$(req POST "/api/admin/articles/$AID/audit" "$JA" '{"pass":false}')
want "驳回不填原因应 422" "HTTP $c" "422"
c=$(req POST "/api/admin/articles/$AID/audit" "$JA" '{"pass":true}')
want "审核通过 PENDING->PUBLISHED" "HTTP $c status=$(jqv "d['data']['status']")" "status=PUBLISHED"
c=$(req POST "/api/admin/articles/$AID/audit" "$JA" '{"pass":true}')
want "重复点通过应 409（CAS 生效）" "HTTP $c" "409"
c=$(req GET "/api/articles/$AID" "")
want "通过后游客可见" "HTTP $c status=$(jqv "d['data']['status']")" "status=PUBLISHED"
c=$(req PUT "/api/articles/$AID" "$JB" "$(body edit.json "{'title':'E2E 改后重审','summary':'','contentMd':'## 改过的正文\n\n编辑后必须重新过审。','categoryId':1,'tagIds':[2],'submit':True}")")
want "改已发布文章 -> PENDING" "HTTP $c status=$(jqv "d['data']['status']")" "status=PENDING"
c=$(req GET "/api/articles/$AID" "")
want "改完游客立刻看不到" "HTTP $c" "404"
c=$(req POST "/api/admin/articles/$AID/audit" "$JA" "$(body rej.json "{'pass':False,'reason':'E2E 驳回原因'}")")
want "驳回并带原因" "HTTP $c status=$(jqv "d['data']['status']")" "status=REJECTED"
c=$(req GET "/api/my/articles/$AID" "$JB")
want "作者能看到驳回原因" "reason=$(jqv "d['data']['rejectReason']")" "E2E 驳回原因"
c=$(req PUT "/api/articles/101" "$JC" "$(body edit.json "{'title':'E2E 越权','summary':'','contentMd':'x','categoryId':1,'tagIds':[],'submit':False}")")
want "小红改小明文章应 403" "HTTP $c code=$(jqv "d['code']")" "403 code=40301"
c=$(req GET "/api/my/articles/101" "$JC")
want "非作者读他人文章应 403" "HTTP $c" "403"
c=$(req POST "/api/articles/$AID/submit" "$JB")
want "驳回后重新提交" "HTTP $c status=$(jqv "d['data']['status']")" "status=PENDING"
c=$(req POST "/api/articles/$AID/withdraw" "$JB")
want "作者撤回 PENDING->DRAFT" "HTTP $c status=$(jqv "d['data']['status']")" "status=DRAFT"
c=$(req POST "/api/admin/articles/$AID/audit" "$JA" '{"pass":true}')
want "撤回后管理员点通过应 409（作业 2.3.3）" "HTTP $c" "409"
c=$(req DELETE "/api/articles/$AID" "$JB")
want "作者删除自己的文章" "HTTP $c" "200"
c=$(req GET "/api/articles/$AID" "$JB")
want "删除后 404" "HTTP $c" "404"

# ---------------------------------------------------------------- 5 评论与审核
c=$(req POST /api/articles/101/comments "$JC" "$(body cmt.json "{'content':'E2E 新评论，等待审核。'}")")
CID=$(jqv "d['data']['id']")
want "小红评论进 PENDING" "HTTP $c status=$(jqv "d['data']['status']")" "status=PENDING"
c=$(req GET /api/articles/101/comments "")
want "未过审评论前台不可见" "top=$(jqv "sorted(x['id'] for x in d['data'])")" "top=[201, 202]"
c=$(req POST "/api/admin/comments/$CID/audit" "$JA" '{"pass":true}')
want "评论审核通过" "HTTP $c status=$(jqv "d['data']['status']")" "status=PUBLISHED"
c=$(req GET /api/articles/101/comments "")
want "过审后前台可见" "top=$(jqv "sorted(x['id'] for x in d['data'])")" "$CID"
c=$(req POST /api/articles/101/comments "$JC" "$(body rep.json "{'content':'E2E 回复内容','parentId':201}")")
RID=$(jqv "d['data']['id']")
want "回复也进 PENDING" "HTTP $c status=$(jqv "d['data']['status']")" "status=PENDING"
c=$(req POST "/api/admin/comments/$RID/audit" "$JA" '{"pass":true}')
want "回复可被审核（审核覆盖回复）" "HTTP $c status=$(jqv "d['data']['status']")" "status=PUBLISHED"
c=$(req GET /api/articles/101/comments "")
want "回复挂到父评论下" "childrenOf201=$(jqv "sorted(x['id'] for p in d['data'] if p['id']==201 for x in p['children'])")" "$RID"
c=$(req GET /api/articles/101 "")
CC=$(jqv "d['data']['commentCount']")
CCSQL=$(sql "SELECT COUNT(*) FROM t_comment WHERE article_id=101 AND status='PUBLISHED' AND deleted=0")
want "评论计数=库中已通过条数" "api=$CC sql=$CCSQL" "$CCSQL"
want "api 与 sql 相等" "$([ "$CC" = "$CCSQL" ] && echo same || echo diff)" "same"
c=$(req POST "/api/admin/comments/$CID/audit" "$JA" '{"pass":true}')
want "重复审核同一条评论应 409" "HTTP $c" "409"
c=$(req POST /api/articles/108/comments "$JC" "$(body cmt2.json "{'content':'文章还没发布'}")")
want "评论未发布文章应 404" "HTTP $c" "404"

# ---------------------------------------------------------------- 6 点赞收藏幂等
c=$(req GET /api/articles/101 "$JB"); L0=$(jqv "d['data']['likeCount']")
c=$(req POST /api/articles/101/like "$JB"); L1=$(jqv "d['data']['count']")
c=$(req POST /api/articles/101/like "$JB"); L2=$(jqv "d['data']['count']")
want "点赞两次：先取消再点赞，回到原值" "$L0->$L1->$L2" "2->1->2"
c=$(req POST /api/articles/101/collect "$JB")
want "收藏开关" "HTTP $c active=$(jqv "d['data']['active']") count=$(jqv "d['data']['count']")" "active=True count=2"
c=$(req GET /api/my/collects "$JB")
want "我的收藏列表（演示数据里已有 102、106）" "HTTP $c total=$(jqv "d['data']['total']")" "total=3"
c=$(req POST /api/articles/101/like "")
want "游客点赞应 401" "HTTP $c" "401"
c=$(req GET /api/articles/101 "$JB")
want "详情带出我的点赞/收藏状态" "liked=$(jqv "d['data']['liked']") collected=$(jqv "d['data']['collected']")" "liked=True collected=True"
LSQL=$(sql "SELECT COUNT(*) FROM t_article_like WHERE article_id=101")
want "点赞计数=关系表行数" "api=$L2 sql=$LSQL" "$LSQL"

# ---------------------------------------------------------------- 7 后台统计与 SQL 对账
c=$(req GET /api/admin/stats "$JA")
want "统计-用户总数对得上 SQL" "api=$(jqv "d['data']['userCount']") sql=$(sql "SELECT COUNT(*) FROM t_user")" "$(sql "SELECT COUNT(*) FROM t_user")"
want "统计-文章总数" "api=$(jqv "d['data']['articleCount']") sql=$(sql "SELECT COUNT(*) FROM t_article")" "$(sql "SELECT COUNT(*) FROM t_article")"
want "统计-已发布数" "api=$(jqv "d['data']['publishedArticleCount']") sql=$(sql "SELECT COUNT(*) FROM t_article WHERE status='PUBLISHED'")" "$(sql "SELECT COUNT(*) FROM t_article WHERE status='PUBLISHED'")"
want "统计-待审核文章数" "api=$(jqv "d['data']['pendingArticleCount']") sql=$(sql "SELECT COUNT(*) FROM t_article WHERE status='PENDING'")" "$(sql "SELECT COUNT(*) FROM t_article WHERE status='PENDING'")"
want "统计-待审核评论数" "api=$(jqv "d['data']['pendingCommentCount']") sql=$(sql "SELECT COUNT(*) FROM t_comment WHERE status='PENDING'")" "$(sql "SELECT COUNT(*) FROM t_comment WHERE status='PENDING'")"
want "统计-阅读量汇总" "api=$(jqv "d['data']['totalViewCount']") sql=$(sql "SELECT SUM(view_count) FROM t_article WHERE deleted=0")" "$(sql "SELECT SUM(view_count) FROM t_article WHERE deleted=0")"
c=$(req GET /api/admin/dashboard "$JA")
want "后台首页含待办任务" "recent=$(jqv "len(d['data']['recentArticles'])") pending=$(jqv "len(d['data']['pendingArticles'])")" "recent=5"
c=$(req GET "/api/admin/articles?status=PENDING" "$JA")
want "后台按状态筛文章" "HTTP $c total=$(jqv "d['data']['total']")" "total=3"
c=$(req GET "/api/admin/articles?authorId=3" "$JA")
want "后台按作者筛文章" "HTTP $c total=$(jqv "d['data']['total']")" "total=5"
c=$(req GET "/api/admin/audit-logs?targetType=ARTICLE" "$JA")
want "审核记录含操作者与说明" "total=$(jqv "d['data']['total']") first=$(jqv "d['data']['records'][0]['action']+'/'+d['data']['records'][0]['operatorName']")" "first=ARTICLE_REJECT/admin"
c=$(req GET /api/admin/stats "$JB")
want "普通用户访问后台应 403" "HTTP $c code=$(jqv "d['code']")" "403 code=40300"
c=$(req GET /api/admin/stats "")
want "游客访问后台应 401" "HTTP $c" "401"

# ---------------------------------------------------------------- 8 下架
c=$(req POST /api/admin/articles/107/offline "$JA" "$(body off.json "{'reason':'E2E 下架原因'}")")
want "下架已发布文章" "HTTP $c status=$(jqv "d['data']['status']")" "status=REMOVED"
c=$(req GET /api/articles/107 "")
want "下架后游客 404" "HTTP $c" "404"
c=$(req GET "/api/my/articles/107" "$JC")
want "作者能看到下架原因" "reason=$(jqv "d['data']['offlineReason']")" "E2E 下架原因"
c=$(req POST /api/admin/articles/107/offline "$JA" "$(body off2.json "{'reason':'重复下架'}")")
want "重复下架应 409" "HTTP $c" "409"
c=$(req POST /api/admin/articles/107/audit "$JA" '{"pass":true}')
want "下架文章直接审核通过应 409" "HTTP $c" "409"
c=$(req POST /api/admin/articles/106/offline "$JA" "$(body off3.json "{'reason':''}")")
want "下架不填原因应 400" "HTTP $c" "400"

# ---------------------------------------------------------------- 9 禁用 / 改密码
c=$(req PATCH /api/admin/users/3/status "$JA" "$(body dis.json "{'status':'DISABLED','remark':'E2E 禁用测试'}")")
want "禁用小红" "HTTP $c status=$(jqv "d['data']['status']")" "status=DISABLED"
c=$(req GET /api/auth/me "$JC")
want "已登录会话立即失效（作业 2.2.1）" "HTTP $c code=$(jqv "d['code']")" "401 code=40102"
c=$(req POST /api/auth/login "$JC" '{"account":"xiaohong","password":"User@123"}')
want "禁用账号不能登录" "HTTP $c code=$(jqv "d['code']")" "401"
c=$(req PATCH /api/admin/users/1/status "$JA" '{"status":"DISABLED"}')
want "管理员禁用自己应 422" "HTTP $c" "422"
c=$(req PATCH /api/admin/users/1/role "$JA" '{"role":"USER"}')
want "降级自己应 422" "HTTP $c" "422"
c=$(req PATCH /api/admin/users/3/status "$JA" "$(body en.json "{'status':'ACTIVE','remark':'恢复'}")")
want "恢复账号" "HTTP $c status=$(jqv "d['data']['status']")" "status=ACTIVE"
c=$(req POST /api/auth/login "$JC" '{"account":"xiaohong","password":"User@123"}')
want "恢复后能登录" "HTTP $c" "200"
c=$(req PUT /api/user/password "$JC" '{"oldPassword":"WRONG","newPassword":"New@12345"}')
want "改密码需校验原密码" "HTTP $c" "400"
c=$(req POST /api/auth/login "$JC2" '{"account":"xiaohong","password":"User@123"}')
want "小红在第二个会话登录（模拟另一台设备）" "HTTP $c" "200"
c=$(req PUT /api/user/password "$JC" '{"oldPassword":"User@123","newPassword":"New@12345"}')
want "改密成功" "HTTP $c code=$(jqv "d['code']")" "200 code=0"
c=$(req GET /api/auth/me "$JC")
want "改密后当前会话被服务端销毁 -> 40100" "HTTP $c code=$(jqv "d['code']")" "401 code=40100"
c=$(req GET /api/auth/me "$JC2")
want "另一台设备的旧会话被凭据版本号挡掉 -> 40104" "HTTP $c code=$(jqv "d['code']")" "401 code=40104"
c=$(req POST /api/auth/login "$JC" '{"account":"xiaohong","password":"New@12345"}')
want "新密码可登录" "HTTP $c" "200"
c=$(req PUT /api/user/password "$JC" '{"oldPassword":"New@12345","newPassword":"User@123"}')
want "改回演示密码" "HTTP $c" "200"
c=$(req GET /api/user/profile "$JC")
want "资料接口也不返回密码" "hasHash=$(jqv "'password' in json.dumps(d).lower()")" "hasHash=False"

# ---------------------------------------------------------------- 10 上传
$PY -c "
import zlib,struct
def ch(t,d): return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
png=b'\x89PNG\r\n\x1a\n'+ch(b'IHDR',struct.pack('>IIBBBBB',1,1,8,2,0,0,0))+ch(b'IDAT',zlib.compress(b'\x00\xff\x00\x00'))+ch(b'IEND',b'')
open('$W/real.png','wb').write(png)
open('$W/fake.png','wb').write(b'this is definitely not a png')
"
c=$(curl -s -o "$W/body.json" -w "%{http_code}" -X POST "$BASE/api/files/image" -b "$JA" -c "$JA" -F "file=@$W/real.png")
URL=$(jqv "d['data']['url']")
want "上传真 PNG" "HTTP $c url=$URL" "200"
c=$(curl -s -o /dev/null -w "%{http_code}" "$BASE$URL")
want "上传的文件能访问" "HTTP $c" "200"
c=$(curl -s -o "$W/body.json" -w "%{http_code}" -X POST "$BASE/api/files/image" -b "$JA" -c "$JA" -F "file=@$W/fake.png")
want "扩展名对但内容不是图片应 400" "HTTP $c" "400"
c=$(curl -s -o "$W/body.json" -w "%{http_code}" -X POST "$BASE/api/files/image" -b "$JA" -c "$JA" -F "file=@$W/fake.png;filename=evil.txt")
want "非图片扩展名应 400" "HTTP $c" "400"
c=$(curl -s -o "$W/body.json" -w "%{http_code}" -X POST "$BASE/api/files/image" -F "file=@$W/real.png")
want "游客上传应 401" "HTTP $c" "401"

# ---------------------------------------------------------------- 11 退出
c=$(req POST /api/auth/logout "$JA")
want "退出登录" "HTTP $c" "200"
c=$(req GET /api/auth/me "$JA")
want "退出后不得继续执行需登录操作" "HTTP $c" "401"
c=$(req GET /api/admin/stats "$JA")
want "退出后后台接口也 401" "HTTP $c" "401"

echo "----------------------------------------" | tee -a "$OUT"
echo "共 $n 项检查，失败 $fail 项" | tee -a "$OUT"
[ "$fail" -eq 0 ] && echo "全部通过" | tee -a "$OUT"
exit "$fail"
