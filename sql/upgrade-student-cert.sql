-- 学生认证: 普通用户填写姓名/年级/专业, 负责人/站长审核通过后方可提交代码
USE `oj`;

ALTER TABLE `user`
    ADD COLUMN `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名(学生认证)',
    ADD COLUMN `grade` VARCHAR(20) DEFAULT NULL COMMENT '年级(如2025级)',
    ADD COLUMN `major` VARCHAR(50) DEFAULT NULL COMMENT '专业',
    ADD COLUMN `cert_status` TINYINT NOT NULL DEFAULT 0 COMMENT '认证状态: 0-未认证, 1-待审核, 2-已通过, 3-已驳回',
    ADD COLUMN `cert_apply_time` DATETIME DEFAULT NULL COMMENT '认证申请时间',
    ADD COLUMN `cert_review_time` DATETIME DEFAULT NULL COMMENT '认证审核时间',
    ADD COLUMN `cert_reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人用户ID',
    ADD COLUMN `cert_reject_reason` VARCHAR(200) DEFAULT NULL COMMENT '驳回原因';
