/**
 * 比赛访问 token 存取(localStorage, 按比赛 ID 隔离)
 */
const KEY_PREFIX = 'oj_contest_token_'

export function getContestToken(id) {
  return localStorage.getItem(KEY_PREFIX + id) || ''
}

export function setContestToken(id, token) {
  localStorage.setItem(KEY_PREFIX + id, token)
}

export function clearContestToken(id) {
  localStorage.removeItem(KEY_PREFIX + id)
}
