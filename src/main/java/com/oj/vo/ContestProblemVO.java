package com.oj.vo;

import lombok.Data;

/**
 * 比赛内题目
 */
@Data
public class ContestProblemVO {

    /** 比赛内题号(如 A) */
    private String displayId;

    private Long problemId;

    private String title;

    /** CF Rating 难度 */
    private Integer difficulty;
}
