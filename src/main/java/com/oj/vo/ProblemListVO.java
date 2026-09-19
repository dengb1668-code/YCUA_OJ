package com.oj.vo;

import com.oj.entity.Problem;
import com.oj.enums.ProblemStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目列表项
 * 列表接口不返回 description/samples 等大字段, 提升分页查询性能
 */
@Data
public class ProblemListVO {

    private Long id;

    private String title;

    /** 题目来源(如: 洛谷 P1001) */
    private String source;

    /** 难度(Codeforces Rating) */
    private Integer difficulty;

    /** 时间限制(毫秒) */
    private Integer timeLimit;

    /** 内存限制(MB) */
    private Integer memoryLimit;

    /** 当前用户的做题状态 */
    private ProblemStatus status;

    private LocalDateTime createTime;

    public static ProblemListVO from(Problem problem) {
        ProblemListVO vo = new ProblemListVO();
        vo.setId(problem.getId());
        vo.setTitle(problem.getTitle());
        vo.setSource(problem.getSource());
        vo.setDifficulty(problem.getDifficulty());
        vo.setTimeLimit(problem.getTimeLimit());
        vo.setMemoryLimit(problem.getMemoryLimit());
        vo.setCreateTime(problem.getCreateTime());
        return vo;
    }
}
