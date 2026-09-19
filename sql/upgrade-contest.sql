-- ============================================================
-- 已有库升级脚本(2026-09-19): 比赛系统
-- 在已上线的库上执行一次即可
-- 用法: mysql -u oj -p oj < upgrade-contest.sql
-- ============================================================
USE `oj`;

-- 1. submission 加比赛关联列
ALTER TABLE `submission` ADD COLUMN `contest_id` BIGINT DEFAULT NULL COMMENT '所属比赛ID(NULL=非比赛提交)' AFTER `problem_id`;
ALTER TABLE `submission` ADD KEY `idx_contest_id` (`contest_id`);

-- 2. 比赛表
CREATE TABLE `contest` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '比赛ID',
    `title`       VARCHAR(100) NOT NULL                COMMENT '比赛标题',
    `description` TEXT         DEFAULT NULL            COMMENT '比赛说明(Markdown)',
    `type`        TINYINT      NOT NULL DEFAULT 0      COMMENT '赛制: 0-ICPC, 1-OI, 2-IOI',
    `start_time`  DATETIME     NOT NULL                COMMENT '开始时间',
    `end_time`    DATETIME     NOT NULL                COMMENT '结束时间',
    `password`    VARCHAR(100) DEFAULT NULL            COMMENT '参赛密码(BCrypt哈希, NULL=公开)',
    `creator_id`  BIGINT       NOT NULL                COMMENT '创建者ID',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_creator` (`creator_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '比赛表';

-- 3. 比赛题目关联表
CREATE TABLE `contest_problem` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `contest_id` BIGINT      NOT NULL                COMMENT '比赛ID',
    `problem_id` BIGINT      NOT NULL                COMMENT '题目ID',
    `display_id` VARCHAR(8)  NOT NULL                COMMENT '比赛内题号(按选题顺序自动生成 A/B/C...)',
    `sort`       INT         NOT NULL DEFAULT 0      COMMENT '排序(题目顺序)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_contest_problem` (`contest_id`, `problem_id`),
    UNIQUE KEY `uk_contest_display` (`contest_id`, `display_id`),
    KEY `idx_contest_sort` (`contest_id`, `sort`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '比赛题目关联表';
