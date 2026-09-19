-- 补充测试题目(不同 CF Rating 难度, 用于展示难度颜色)
USE oj;

-- 1. A-B Problem (Rating 800, 灰色)
INSERT INTO problem (title, source, description, input_description, output_description, samples, time_limit, memory_limit, difficulty)
VALUES (
  'A-B Problem',
  '原创',
  '## 题目描述\n\n输入两个整数 a 和 b，输出它们的差 a - b。\n\n## 提示\n\n- 数据范围: -10^9 <= a, b <= 10^9\n- 与 A+B Problem 类似，注意负数的处理',
  '一行两个整数 a 和 b，用空格分隔。',
  '输出一个整数，表示 a - b 的值。',
  '[{"input":"5 3","output":"2","explanation":"5 - 3 = 2。"},{"input":"-1 4","output":"-5","explanation":"负数相减：-1 - 4 = -5。"}]',
  1000, 128, 800
);

-- 2. 回文数判断 (Rating 1000, 灰色)
INSERT INTO problem (title, source, description, input_description, output_description, samples, time_limit, memory_limit, difficulty)
VALUES (
  '回文数判断',
  '原创',
  '## 题目描述\n\n判断一个正整数是否是回文数。回文数是指正着读和倒着读都一样的数，例如 121、12321。\n\n## 提示\n\n- 数据范围: 1 <= n <= 10^9\n- 可以转换成字符串后双指针判断，也可以数学方法反转数字',
  '一行一个正整数 n。',
  '是回文数输出 `Yes`，否则输出 `No`。',
  '[{"input":"121","output":"Yes","explanation":"121 倒过来读还是 121。"},{"input":"123","output":"No","explanation":"123 倒过来是 321，不相等。"}]',
  1000, 128, 1000
);

-- 3. 最大子段和 (Rating 1300, 绿色)
INSERT INTO problem (title, source, description, input_description, output_description, samples, time_limit, memory_limit, difficulty)
VALUES (
  '最大子段和',
  '原创',
  '## 题目描述\n\n给定一个长度为 n 的整数序列，求它的一个连续子段，使得该子段的和最大，输出这个最大和。\n\n## 提示\n\n- 数据范围: 1 <= n <= 2 * 10^5，|a[i]| <= 10^4\n- 经典动态规划：令 dp[i] 表示以第 i 个数结尾的最大子段和，则 dp[i] = max(a[i], dp[i-1] + a[i])\n- 答案可能为负数（全负序列时取最大的单个元素）',
  '第一行一个整数 n，表示序列长度。\n\n第二行 n 个整数，表示序列。',
  '输出一个整数，表示最大子段和。',
  '[{"input":"6\\n-2 1 -3 4 -1 2 1","output":"6","explanation":"选取子段 [4, -1, 2, 1]，和为 6。"},{"input":"3\\n-1 -2 -3","output":"-1","explanation":"全为负数，最大子段和只能取最大的单个元素 -1。"}]',
  1000, 256, 1300
);
