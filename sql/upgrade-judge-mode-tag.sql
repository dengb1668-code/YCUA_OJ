-- ============================================================
-- 已有库升级脚本(2026-09-20): 题目级判题模式 + 题目标签 + CF 式失败测试点
-- 在已上线的库上执行一次即可
-- 用法: mysql -u oj -p oj < upgrade-judge-mode-tag.sql
-- ============================================================
USE `oj`;

-- 1. problem 加判题模式列
--    存量题回填 IOI(1), 保持原有洛谷式逐点部分分行为;
--    新建题走 DB 默认值 ICPC(0, 首错即停)
ALTER TABLE `problem` ADD COLUMN `judge_mode` TINYINT NOT NULL DEFAULT 0 COMMENT '判题模式: 0-ICPC(首错即停), 1-IOI(部分分)' AFTER `difficulty`;
UPDATE `problem` SET `judge_mode` = 1;

-- 2. 题目标签表(标签集为后端常量, 不做字典表)
CREATE TABLE `problem_tag` (
    `problem_id` BIGINT      NOT NULL COMMENT '题目ID',
    `tag`        VARCHAR(20) NOT NULL COMMENT '标签(中文, 后端常量白名单)',
    PRIMARY KEY (`problem_id`, `tag`),
    KEY `idx_tag` (`tag`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '题目标签表';

-- 3. submission 加失败测试点序号列
--    ICPC 模式首个失败测试点, 列表页直接显示 "Wrong answer on test N", 免解析 judge_detail JSON
ALTER TABLE `submission` ADD COLUMN `failed_test_index` INT DEFAULT NULL COMMENT 'ICPC 模式首个失败测试点序号' AFTER `score`;
