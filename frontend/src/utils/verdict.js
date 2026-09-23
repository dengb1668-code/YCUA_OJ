/**
 * 判定状态与语言展示(全站共用)
 */

// AtCoder 式判定配色
const verdictMap = {
  ACCEPTED: { short: 'AC', label: 'Accepted', color: '#5cb85c' },
  WRONG_ANSWER: { short: 'WA', label: 'Wrong Answer', color: '#f0ad4e' },
  TIME_LIMIT_EXCEEDED: { short: 'TLE', label: 'Time Limit Exceeded', color: '#e67e22' },
  MEMORY_LIMIT_EXCEEDED: { short: 'MLE', label: 'Memory Limit Exceeded', color: '#e67e22' },
  RUNTIME_ERROR: { short: 'RE', label: 'Runtime Error', color: '#d9534f' },
  COMPILE_ERROR: { short: 'CE', label: 'Compile Error', color: '#999' },
  SYSTEM_ERROR: { short: 'SE', label: 'System Error', color: '#d9534f' },
  PENDING: { short: 'Pending', label: '等待判题', color: '#999' },
  JUDGING: { short: 'Judging', label: '判题中', color: '#999' }
}

export function verdictOf(status) {
  return verdictMap[status] ?? { short: status, label: status, color: '#999' }
}

/**
 * CF 式判定文案: 带失败测试点显示 "Wrong answer on test 3";
 * failedTestIndex 为空(IOI 模式/旧数据)回退 label
 */
export function verdictText(status, failedTestIndex) {
  const v = verdictOf(status)
  if (failedTestIndex == null) return v.label
  return `${v.label} on test ${failedTestIndex}`
}

const languageMap = {
  CPP: 'C++',
  JAVA: 'Java',
  C: 'C',
  PYTHON3: 'Python3',
  GO: 'Go',
  JAVASCRIPT: 'JavaScript'
}

export function languageLabel(language) {
  return languageMap[language] ?? language
}

/** ISO 时间转 'YYYY-MM-DD HH:mm:ss' */
export function formatTime(iso) {
  if (!iso) return '—'
  return String(iso).replace('T', ' ').slice(0, 19)
}
