-- ============================================================
-- OJ 系统核心表结构（MySQL 8.x, utf8mb4）
-- 说明:
--   1. 三张表之间不建外键约束, 由应用层保证数据一致性
--      (OJ 提交量大, 外键会拖慢写入并影响判题吞吐)
--   2. 枚举字段(role/difficulty/language/status)均用 TINYINT 存储,
--      与 Java 枚举通过 MyBatis-Plus @EnumValue 映射
-- ============================================================

CREATE DATABASE IF NOT EXISTS `oj` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `oj`;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(50)  NOT NULL                COMMENT '用户名(唯一)',
    `password`    VARCHAR(255) NOT NULL                COMMENT '密码(BCrypt哈希)',
    `nickname`    VARCHAR(50)  DEFAULT NULL            COMMENT '昵称',
    `phone`       VARCHAR(20)  DEFAULT NULL            COMMENT '手机号(唯一, 找回密码依据)',
    `email`       VARCHAR(100) DEFAULT NULL            COMMENT '邮箱',
    `avatar`      VARCHAR(255) DEFAULT NULL            COMMENT '头像URL',
    `role`        TINYINT      NOT NULL DEFAULT 0      COMMENT '角色: 0-普通用户, 1-管理员, 2-站长',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';

-- ------------------------------------------------------------
-- 2. 题目表
-- ------------------------------------------------------------
CREATE TABLE `problem` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    `title`              VARCHAR(100) NOT NULL                COMMENT '题目标题',
    `source`             VARCHAR(100) DEFAULT NULL            COMMENT '题目来源(如: 洛谷 P1001)',
    `description`        TEXT         NOT NULL                COMMENT '题目描述(Markdown)',
    `input_description`  TEXT         DEFAULT NULL            COMMENT '输入格式说明',
    `output_description` TEXT         DEFAULT NULL            COMMENT '输出格式说明',
    `samples`            TEXT         DEFAULT NULL            COMMENT '样例, JSON数组: [{"input":"...","output":"..."}]',
    `time_limit`         INT          NOT NULL DEFAULT 1000   COMMENT '时间限制(毫秒)',
    `memory_limit`       INT          NOT NULL DEFAULT 256    COMMENT '内存限制(MB)',
    `difficulty`         INT          NOT NULL DEFAULT 800    COMMENT '难度(Codeforces Rating, 800-3500)',
    `judge_mode`         TINYINT      NOT NULL DEFAULT 0      COMMENT '判题模式: 0-ICPC(首错即停), 1-IOI(部分分)',
    `author_id`          BIGINT       DEFAULT NULL            COMMENT '创建者用户ID(测试点管理权限依据)',
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '题目表';

-- 已有库升级(测试点功能新增 author_id 列):
-- ALTER TABLE problem ADD COLUMN author_id BIGINT DEFAULT NULL COMMENT '创建者用户ID' AFTER difficulty;
-- 已有库升级(计分制新增 score 列):
-- ALTER TABLE submission ADD COLUMN score INT DEFAULT NULL COMMENT '得分(计分制)' AFTER status;

-- ------------------------------------------------------------
-- 3. 提交记录表
-- ------------------------------------------------------------
CREATE TABLE `submission` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '提交ID',
    `user_id`       BIGINT      NOT NULL                COMMENT '提交用户ID',
    `problem_id`    BIGINT      NOT NULL                COMMENT '题目ID',
    `contest_id`    BIGINT      DEFAULT NULL            COMMENT '所属比赛ID(NULL=非比赛提交)',
    `language`      TINYINT     NOT NULL                COMMENT '编程语言: 0-Java, 1-C++, 2-C, 3-Python3, 4-Go, 5-JavaScript',
    `code`          MEDIUMTEXT  NOT NULL                COMMENT '提交的源代码',
    `status`        TINYINT     NOT NULL DEFAULT 0      COMMENT '判题状态: 0-Pending, 1-Judging, 2-AC, 3-WA, 4-TLE, 5-MLE, 6-RE, 7-CE, 8-SE',
    `score`         INT         DEFAULT NULL            COMMENT '得分(计分制, 满分=题目测试点分值总和)',
    `failed_test_index` INT     DEFAULT NULL            COMMENT 'ICPC 模式首个失败测试点序号',
    `time_used`     INT         DEFAULT NULL            COMMENT '运行耗时(毫秒)',
    `memory_used`   INT         DEFAULT NULL            COMMENT '运行内存(KB)',
    `error_message` TEXT        DEFAULT NULL            COMMENT '错误信息(编译错误/运行时错误输出)',
    `judge_detail`  TEXT        DEFAULT NULL            COMMENT '逐样例判题结果(JSON数组)',
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `update_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '判题完成时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_problem` (`user_id`, `problem_id`),
    KEY `idx_problem_id` (`problem_id`),
    KEY `idx_contest_id` (`contest_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '提交记录表';

-- 已有库升级(比赛系统新增 contest_id 列):
-- ALTER TABLE submission ADD COLUMN contest_id BIGINT DEFAULT NULL COMMENT '所属比赛ID' AFTER problem_id;
-- ALTER TABLE submission ADD KEY idx_contest_id (contest_id);

-- ------------------------------------------------------------
-- 3.5 题目标签表(标签集为后端常量, 不做字典表)
-- ------------------------------------------------------------
CREATE TABLE `problem_tag` (
    `problem_id` BIGINT      NOT NULL COMMENT '题目ID',
    `tag`        VARCHAR(20) NOT NULL COMMENT '标签(中文, 后端常量白名单)',
    PRIMARY KEY (`problem_id`, `tag`),
    KEY `idx_tag` (`tag`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '题目标签表';

-- ------------------------------------------------------------
-- 4. 帖子表(讨论帖/题解统一建模)
-- ------------------------------------------------------------
CREATE TABLE `post` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
    `problem_id`  BIGINT       DEFAULT NULL            COMMENT '关联题目ID(NULL=全局讨论)',
    `type`        TINYINT      NOT NULL DEFAULT 0      COMMENT '类型: 0-讨论, 1-题解',
    `user_id`     BIGINT       NOT NULL                COMMENT '发帖人ID',
    `title`       VARCHAR(100) NOT NULL                COMMENT '标题',
    `content`     TEXT         NOT NULL                COMMENT '正文(Markdown)',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_problem_type` (`problem_id`, `type`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '帖子表(讨论/题解)';

-- ------------------------------------------------------------
-- 5. 回复表(帖子楼层回复)
-- ------------------------------------------------------------
CREATE TABLE `reply` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '回复ID',
    `post_id`     BIGINT   NOT NULL                COMMENT '所属帖子ID',
    `user_id`     BIGINT   NOT NULL                COMMENT '回复人ID',
    `content`     TEXT     NOT NULL                COMMENT '回复内容(Markdown)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '帖子楼层回复表';

-- ------------------------------------------------------------
-- 6. 比赛表
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 7. 比赛题目关联表
-- ------------------------------------------------------------
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
