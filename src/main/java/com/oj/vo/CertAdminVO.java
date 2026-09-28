package com.oj.vo;

import com.oj.enums.CertStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生认证申请条目(负责人/站长审核列表)
 */
@Data
public class CertAdminVO {

    private Long userId;

    private String username;

    private String nickname;

    /** 真实姓名 */
    private String realName;

    /** 年级 */
    private String grade;

    /** 专业 */
    private String major;

    /** 认证状态 */
    private CertStatus certStatus;

    /** 申请时间 */
    private LocalDateTime certApplyTime;
}
