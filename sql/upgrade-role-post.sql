-- ============================================================
-- 已有库升级脚本(2026-09-19): 三角色权限 + 讨论区/题解 + 手机号
-- 在已上线的库上执行一次即可(幂等不保证, 勿重复执行建表部分)
-- 用法: mysql -u oj -p oj < upgrade-role-post.sql
-- ============================================================
USE `oj`;

-- 1. 用户表加手机号(唯一, 旧用户为 NULL, MySQL 唯一索引允许多个 NULL)
ALTER TABLE `user` ADD COLUMN `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号(唯一, 找回密码依据)' AFTER `nickname`;
ALTER TABLE `user` ADD UNIQUE KEY `uk_phone` (`phone`);

-- 2. 帖子表(讨论帖/题解)
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

-- 3. 回复表(楼层回复)
CREATE TABLE `reply` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '回复ID',
    `post_id`     BIGINT   NOT NULL                COMMENT '所属帖子ID',
    `user_id`     BIGINT   NOT NULL                COMMENT '回复人ID',
    `content`     TEXT     NOT NULL                COMMENT '回复内容(Markdown)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '帖子楼层回复表';

-- 4. 站主初始化: hermanO 设为站长(role=2)
UPDATE `user` SET `role` = 2 WHERE `username` = 'hermanO';

-- 5. 旧题归属: author_id 为空的题目(如 A+B)归站长名下, 收紧权限后仅站长/管理员可管理
UPDATE `problem` SET `author_id` = (SELECT `id` FROM `user` WHERE `username` = 'hermanO') WHERE `author_id` IS NULL;
