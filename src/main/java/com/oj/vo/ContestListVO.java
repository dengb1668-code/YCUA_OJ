package com.oj.vo;

import com.oj.enums.ContestType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 比赛列表项
 */
@Data
public class ContestListVO {

    private Long id;

    private String title;

    private ContestType type;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 是否有参赛密码 */
    private Boolean hasPassword;

    /** 状态: NOT_STARTED / RUNNING / ENDED(由时间派生) */
    private String status;

    /** 创建者昵称 */
    private String creatorName;
}
