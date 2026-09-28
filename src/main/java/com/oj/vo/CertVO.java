package com.oj.vo;

import com.oj.enums.CertStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的学生认证信息(个人页展示)
 */
@Data
public class CertVO {

    /** 认证状态: NONE/PENDING/APPROVED/REJECTED */
    private CertStatus certStatus;

    /** 真实姓名 */
    private String realName;

    /** 年级 */
    private String grade;

    /** 专业 */
    private String major;

    /** 申请时间 */
    private LocalDateTime certApplyTime;

    /** 审核时间 */
    private LocalDateTime certReviewTime;

    /** 驳回原因 */
    private String certRejectReason;
}
