package com.oj.vo;

import lombok.Data;

import java.util.List;

/**
 * 个人主页统计(Codeforces 风格):
 * 总提交 / 通过题数 / 未通过题数 / 每日提交柱状图数据
 */
@Data
public class UserStatsVO {

    /** 总提交数 */
    private Long totalSubmissions;

    /** 通过的提交数 */
    private Long acceptedSubmissions;

    /** 已通过的题目数(去重) */
    private Long solvedProblems;

    /** 尝试过但未通过的题目数(去重) */
    private Long attemptedProblems;

    /** 提交通过率, 如 "62.5%" */
    private String acRate;

    /** 最近 365 天每日 AC 提交数(CF 年度格子图) */
    private List<DailyCount> dailyAccepted;

    /** 已解决题目的 rating 分布 */
    private List<RatingCount> ratingDistribution;

    @Data
    public static class DailyCount {
        private String date;
        private Long count;
    }

    @Data
    public static class RatingCount {
        private Integer rating;
        private Long count;
    }
}
