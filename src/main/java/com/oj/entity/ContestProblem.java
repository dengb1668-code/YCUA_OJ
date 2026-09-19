package com.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 比赛题目关联实体, 对应表 contest_problem
 */
@Data
@TableName("contest_problem")
public class ContestProblem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 比赛ID */
    private Long contestId;

    /** 题目ID */
    private Long problemId;

    /** 比赛内题号(按选题顺序自动生成 A/B/C...) */
    private String displayId;

    /** 排序 */
    private Integer sort;
}
