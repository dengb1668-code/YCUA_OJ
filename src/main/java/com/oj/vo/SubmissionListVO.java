package com.oj.vo;

import com.oj.enums.JudgeStatus;
import com.oj.enums.Language;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提交记录列表项(含题目标题, 不含代码等大字段)
 */
@Data
public class SubmissionListVO {

    private Long id;

    private Long problemId;

    private String problemTitle;

    /** 所属比赛ID(NULL=非比赛提交) */
    private Long contestId;

    /** 比赛内题号(如 A; 仅比赛提交有) */
    private String displayId;

    /** 提交用户ID */
    private Long userId;

    /** 提交用户名(昵称优先) */
    private String username;

    /** 当前用户是否可查看该提交的代码(本人 ∨ 管理端 ∨ 对该题已 AC) */
    private Boolean canViewCode;

    private Language language;

    private JudgeStatus status;

    /** 得分(计分制) */
    private Integer score;

    /** ICPC 模式首个失败测试点序号(显示 "Wrong answer on test N" 用) */
    private Integer failedTestIndex;

    /** 运行耗时(毫秒), CE 等未运行场景为 null */
    private Integer timeUsed;

    private Integer memoryUsed;

    private LocalDateTime createTime;
}
