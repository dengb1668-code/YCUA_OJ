/* OJ UI 视觉验证脚本: 用自签 JWT 造登录态, 逐页截图(亮/暗两套) */
const { chromium } = require('playwright')
const crypto = require('crypto')

const BASE = 'http://localhost:5173'
const SECRET = 'oj-shell-dev-secret-key-0123456789-abcdefghijklmnopqrstuvwxyz'

function b64url(buf) {
  return Buffer.from(buf).toString('base64url')
}

function makeToken(userId, username) {
  const header = b64url(JSON.stringify({ alg: 'HS256' }))
  const now = Math.floor(Date.now() / 1000)
  const payload = b64url(
    JSON.stringify({ sub: String(userId), username, iat: now, exp: now + 86400 })
  )
  const sig = crypto
    .createHmac('sha256', SECRET)
    .update(`${header}.${payload}`)
    .digest('base64url')
  return `${header}.${payload}.${sig}`
}

// 页面清单: [路径, 名称]
const PAGES = [
  ['/problems', 'problem-list'],
  ['/problems/1', 'problem-detail'],
  ['/submissions', 'submissions'],
  ['/profile', 'profile'],
  ['/contests', 'contest-list'],
  ['/contests/1', 'contest-detail'],
  ['/discussion', 'discussion'],
  ['/login', 'login']
]

async function shot(page, theme, url, name, dir) {
  await page.evaluate((t) => {
    localStorage.setItem('oj_theme', t)
  }, theme)
  await page.reload({ waitUntil: 'networkidle' })
  await page.waitForTimeout(600)
  await page.screenshot({ path: `${dir}/${theme}-${name}.png`, fullPage: false })
  console.log(`shot: ${theme}-${name}`)
}

;(async () => {
  const browser = await chromium.launch({ channel: 'msedge' })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })

  // 登录态: 用 userId=1 签一个 24h token
  const token = makeToken(1, 'admin')
  await page.goto(BASE + '/login', { waitUntil: 'networkidle' })
  await page.evaluate((t) => {
    localStorage.setItem('oj_token', t)
    localStorage.setItem('oj_username', 'admin')
    localStorage.setItem('oj_userId', '1')
    localStorage.setItem('oj_role', 'OWNER')
    localStorage.setItem('oj_theme', 'light')
  }, token)

  const dir = 'shots'
  const fs = require('fs')
  fs.mkdirSync(dir, { recursive: true })

  for (const [url, name] of PAGES) {
    await page.goto(BASE + url, { waitUntil: 'networkidle' })
    await page.waitForTimeout(800)
    await page.screenshot({ path: `${dir}/light-${name}.png` })
    console.log(`shot: light-${name}`)
  }

  // 暗色
  for (const [url, name] of PAGES) {
    await page.evaluate(() => localStorage.setItem('oj_theme', 'dark'))
    await page.goto(BASE + url, { waitUntil: 'networkidle' })
    await page.waitForTimeout(800)
    await page.screenshot({ path: `${dir}/dark-${name}.png` })
    console.log(`shot: dark-${name}`)
  }

  await browser.close()
  console.log('DONE')
})().catch((e) => {
  console.error('FAIL', e)
  process.exit(1)
})
