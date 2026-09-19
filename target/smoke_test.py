# -*- coding: utf-8 -*-
"""新功能冒烟测试: 三角色权限 + 讨论/题解/回复 + 手机号找回密码 (本地 8088)"""
import json
import sys
import urllib.request

import redis

BASE = "http://localhost:8088/api"
r = redis.Redis(host="localhost", port=6379, decode_responses=True, protocol=2)

passed, failed = 0, 0


def check(name, cond, extra=""):
    global passed, failed
    if cond:
        passed += 1
        print(f"  PASS  {name}")
    else:
        failed += 1
        print(f"  FAIL  {name}  {extra}")


def call(method, path, body=None, token=None, raw=False):
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        try:
            payload = json.loads(e.read().decode())
        except Exception:
            payload = {}
        return e.code, payload


def login(username, password):
    """自动从 Redis 取验证码完成登录"""
    _, cap = call("GET", "/captcha")
    code = r.get("oj:captcha:" + cap["data"]["captchaId"])
    assert code, "captcha not found in redis"
    status, resp = call("POST", "/user/login",
                        {"username": username, "password": password,
                         "captchaId": cap["data"]["captchaId"], "captchaCode": code})
    assert status == 200, f"login {username} failed: {resp}"
    return resp["data"]


print("== 1. 登录与角色 ==")
owner = login("tester", "123456")   # 本地库已临时把 tester 设为站长
check("LoginVO 返回 role", owner.get("role") == "OWNER", str(owner))

print("== 2. 注册(含手机号) ==")
import random
suffix = random.randint(1000, 9999)
status, resp = call("POST", "/user/register",
                    {"username": f"smoke{suffix}", "password": "abc12345", "phone": f"139{suffix}0000"})
check("注册成功并自动登录", status == 200 and resp["data"]["role"] == "USER", str(resp))
ua = resp["data"]
phone = f"139{suffix}0000"

status, resp = call("POST", "/user/register",
                    {"username": f"smoke2{suffix}", "password": "abc12345", "phone": phone})
check("手机号重复注册被拒", resp.get("code") == 400, str(resp))

status, resp = call("POST", "/user/register",
                    {"username": f"smoke3{suffix}", "password": "abc12345", "phone": "12345"})
check("手机号格式校验", resp.get("code") == 400, str(resp))

print("== 3. 用户管理(仅站长) ==")
status, resp = call("GET", "/user/page", token=ua["token"])
check("普通用户调用户管理被拒", resp.get("code") == 400, str(resp))
status, resp = call("GET", "/user/page", token=owner["token"])
check("站长分页查用户", status == 200 and len(resp["data"]["records"]) >= 2, str(resp)[:120])
smoke_id = None
for u in resp["data"]["records"]:
    if u["username"] == f"smoke{suffix}":
        smoke_id = u["id"]
        break
status, resp = call("PUT", f"/user/{smoke_id}/role", {"role": 1}, token=owner["token"])
check("站长设管理员", status == 200, str(resp))
_, cap = call("GET", "/captcha")
code = r.get("oj:captcha:" + cap["data"]["captchaId"])
_, relogin = call("POST", "/user/login",
                  {"username": f"smoke{suffix}", "password": "abc12345",
                   "captchaId": cap["data"]["captchaId"], "captchaCode": code})
check("重新登录后 role=ADMIN", relogin["data"]["role"] == "ADMIN", str(relogin)[:120])
ua = relogin["data"]
status, resp = call("PUT", f"/user/{owner['userId']}/role", {"role": 0}, token=ua["token"])
check("管理员不能改站长", resp.get("code") == 400, str(resp))
status, resp = call("PUT", "/user/999999/role", {"role": 1}, token=owner["token"])
check("用户不存在报错", resp.get("code") == 400, str(resp))

print("== 4. 题目权限收紧 ==")
status, resp = call("POST", "/problem",
                    {"title": "smoke problem", "description": "d", "timeLimit": 1000,
                     "memoryLimit": 256, "difficulty": 800}, token=ua["token"])
check("创建题目", status == 200, str(resp)[:100])
pid = resp["data"]
# 用 tester(站长) 编辑 smoke 用户的题目 → 允许
status, resp = call("PUT", f"/problem/{pid}",
                    {"title": "edited", "description": "d", "timeLimit": 1000,
                     "memoryLimit": 256, "difficulty": 800}, token=owner["token"])
check("站长可编辑他人题目", status == 200, str(resp)[:100])
# 普通用户编辑他人题目 → 拒绝(再注册一个普通用户)
status, resp = call("POST", "/user/register",
                    {"username": f"plain{suffix}", "password": "abc12345", "phone": f"137{suffix}0000"})
plain = resp["data"]
status, resp = call("PUT", f"/problem/{pid}",
                    {"title": "hack", "description": "d", "timeLimit": 1000,
                     "memoryLimit": 256, "difficulty": 800}, token=plain["token"])
check("普通用户编辑他人题目被拒", resp.get("code") == 400, str(resp)[:100])

print("== 5. 讨论/题解/回复 ==")
status, resp = call("POST", "/post",
                    {"type": "DISCUSSION", "problemId": None, "title": "全局讨论帖",
                     "content": "# hello **world**"}, token=ua["token"])
check("发全局讨论帖", status == 200, str(resp)[:100])
post_id = resp["data"]
status, resp = call("POST", "/post",
                    {"type": "SOLUTION", "problemId": None, "title": "x", "content": "y"},
                    token=ua["token"])
check("题解必须关联题目", resp.get("code") == 400, str(resp)[:100])
status, resp = call("POST", "/post",
                    {"type": "SOLUTION", "problemId": pid, "title": "题解帖", "content": "code here"},
                    token=ua["token"])
check("发题解", status == 200, str(resp)[:100])
sol_id = resp["data"]
status, resp = call("GET", "/post/page", token=ua["token"])
check("全局讨论分页", status == 200, str(resp)[:100])
status, resp = call("GET", f"/post/page?type=SOLUTION&problemId={pid}", token=ua["token"])
check("某题题解分页", status == 200 and resp["data"]["total"] >= 1, str(resp)[:100])
status, resp = call("GET", f"/post/{post_id}", token=ua["token"])
check("帖子详情(作者可编辑)", status == 200 and resp["data"]["canEdit"] is True
      and resp["data"]["problemTitle"] is None, str(resp)[:120])
status, resp = call("POST", f"/post/{post_id}/reply", {"content": "楼层回复"}, token=owner["token"])
check("回复帖子", status == 200, str(resp)[:100])
reply_id = resp["data"]
status, resp = call("GET", f"/post/{post_id}", token=plain["token"])
check("详情含回复且他人不可删", status == 200 and len(resp["data"]["replies"]) == 1
      and resp["data"]["replies"][0]["canDelete"] is False, str(resp)[:120])
status, resp = call("PUT", f"/post/{post_id}", {"title": "hack", "content": "hack"},
                    token=plain["token"])
check("非作者非管理不可编辑", resp.get("code") == 400, str(resp)[:100])
status, resp = call("DELETE", f"/reply/{reply_id}", token=ua["token"])
check("管理员可删他人回复", status == 200, str(resp)[:100])
status, resp = call("DELETE", f"/post/{sol_id}", token=owner["token"])
check("管理端可删他人帖", status == 200, str(resp)[:100])

print("== 6. 手机号找回密码 ==")
_, cap = call("GET", "/captcha")
code = r.get("oj:captcha:" + cap["data"]["captchaId"])
status, resp = call("POST", "/user/forgot-password",
                    {"username": f"plain{suffix}", "phone": f"137{suffix}0000",
                     "captchaId": cap["data"]["captchaId"], "captchaCode": code,
                     "newPassword": "newpass999"})
check("找回密码", status == 200, str(resp)[:100])
_, cap = call("GET", "/captcha")
code = r.get("oj:captcha:" + cap["data"]["captchaId"])
status, resp = call("POST", "/user/login",
                    {"username": f"plain{suffix}", "password": "newpass999",
                     "captchaId": cap["data"]["captchaId"], "captchaCode": code})
check("新密码可登录", status == 200, str(resp)[:100])
_, cap = call("GET", "/captcha")
code = r.get("oj:captcha:" + cap["data"]["captchaId"])
status, resp = call("POST", "/user/forgot-password",
                    {"username": f"plain{suffix}", "phone": "13800000000",
                     "captchaId": cap["data"]["captchaId"], "captchaCode": code,
                     "newPassword": "newpass999"})
check("手机号不匹配被拒", resp.get("code") == 400, str(resp)[:100])

print(f"\n===== 结果: {passed} 通过, {failed} 失败 =====")
sys.exit(1 if failed else 0)
