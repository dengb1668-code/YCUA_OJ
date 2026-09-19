package com.oj.vo;

import com.oj.enums.ContestType;
import lombok.Data;

import java.util.List;

/**
 * 比赛榜单
 */
@Data
public class StandingsVO {

    /** OI 赛制比赛进行中且非创建者/管理端时为 true(榜单隐藏) */
    private Boolean hidden;

    private ContestType type;

    /** 比赛内题目列表(表头) */
    private List<ContestProblemVO> problems;

    /** 每题统计: ICPC=solvedCount, OI/IOI=null(每行有得分) */
    private List<Long> problemSolvedCount;

    private List<Row> rows;

    @Data
    public static class Row {
        /** 名次(并列同名次, 如 1,1,3) */
        private Integer rank;
        private Long userId;
        private String username;

        // ICPC 用
        /** 解题数 */
        private Integer solved;
        /** 总罚时(分钟) */
        private Integer penalty;
        /** 每题状态: "AC 2" / "WA 1" / null(未提交) 等 */
        private List<String> problemStates;

        // OI/IOI 用
        /** 总分 */
        private Integer totalScore;
        /** 每题最高得分 */
        private List<Integer> problemScores;
    }
}
