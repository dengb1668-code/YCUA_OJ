-- 题目发布状态与博客类型升级
USE `oj`;

ALTER TABLE `problem`
    ADD COLUMN `publish_status` TINYINT NOT NULL DEFAULT 1
        COMMENT '题目发布状态: 0-草稿, 1-已发布, 2-已归档'
        AFTER `difficulty`;

-- 旧题目保持原有可见性；新建题目由后端写入草稿状态。
UPDATE `problem` SET `publish_status` = 1 WHERE `publish_status` IS NULL;

-- post.type: 0-讨论, 1-题解, 2-博客
