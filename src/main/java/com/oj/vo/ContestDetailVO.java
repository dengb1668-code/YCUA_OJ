package com.oj.vo;

import com.oj.enums.ContestType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 比赛详情(不含题目列表; 题目走带 token 的 /problems 接口)
 */
@Data
public class ContestDetailVO {

    private Long id;

    private String title;

    private String description;

    private ContestType type;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 是否有参赛密码 */
    private Boolean hasPassword;

    /** 状态: NOT_STARTED / RUNNING / ENDED */
    private String status;

    /** 创建者昵称 */
    private String creatorName;

    /** 当前用户是否为创建者或管理端 */
    private Boolean canManage;

    /** 当前用户是否已可进入(公开赛/已持有有效 token/创建者/管理端) */
    private Boolean canEnter;

    /** 题数(仅已开赛或 canManage 时返回, 否则 null) */
    private Integer problemCount;

    /** 比赛内题目列表(仅已开赛或 canManage 时返回, 否则 null) */
    private List<ContestProblemVO> problems;
}
