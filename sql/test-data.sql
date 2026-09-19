-- 测试数据: A+B Problem
USE oj;

INSERT INTO problem (title, description, input_description, output_description, samples, time_limit, memory_limit, difficulty)
VALUES (
  'A+B Problem',
  '## 题目描述\n\n输入两个整数 a 和 b，输出它们的和。\n\n## 提示\n\n- 数据范围: -10^9 <= a, b <= 10^9\n- C++ 中请使用 `long long` 类型避免溢出\n- 读入后直接输出 `a + b` 即可',
  '一行两个整数 a 和 b，用空格分隔。',
  '输出一个整数，表示 a + b 的值。',
  '[{"input":"1 2","output":"3","explanation":"1 + 2 = 3，直接输出两数之和即可。"},{"input":"100 200","output":"300","explanation":"100 + 200 = 300。"},{"input":"-5 5","output":"0","explanation":"注意负数相加的情况：-5 + 5 = 0。"}]',
  1000,
  256,
  0
);
