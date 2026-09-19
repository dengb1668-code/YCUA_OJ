package com.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oj.entity.Submission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {

    @Select("SELECT COUNT(*) FROM submission WHERE user_id = #{userId}")
    long countTotalByUser(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM submission WHERE user_id = #{userId} AND status = #{acceptedCode}")
    long countAcceptedByUser(@Param("userId") Long userId, @Param("acceptedCode") int acceptedCode);

    /** 已通过题目数(去重) */
    @Select("SELECT COUNT(DISTINCT problem_id) FROM submission WHERE user_id = #{userId} AND status = #{acceptedCode}")
    long countSolvedProblems(@Param("userId") Long userId, @Param("acceptedCode") int acceptedCode);

    /** 尝试过但未通过的题目数(去重) */
    @Select("SELECT COUNT(DISTINCT problem_id) FROM submission WHERE user_id = #{userId} AND status != #{acceptedCode} " +
            "AND problem_id NOT IN (SELECT DISTINCT problem_id FROM submission WHERE user_id = #{userId} AND status = #{acceptedCode})")
    long countUnsolvedAttempted(@Param("userId") Long userId, @Param("acceptedCode") int acceptedCode);

    /** 最近 365 天每日 AC 提交数(CF 年度格子图数据源); GROUP BY 必须与 SELECT 表达式一致, 否则触发 only_full_group_by */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS date, COUNT(*) AS cnt " +
            "FROM submission WHERE user_id = #{userId} AND status = #{acceptedCode} " +
            "AND create_time >= DATE_SUB(CURDATE(), INTERVAL 364 DAY) " +
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d') ORDER BY date")
    List<Map<String, Object>> dailyAcceptedCounts(@Param("userId") Long userId, @Param("acceptedCode") int acceptedCode);

    /** 已解决题目的 rating 分布(去重按题) */
    @Select("SELECT p.difficulty AS rating, COUNT(DISTINCT s.problem_id) AS cnt " +
            "FROM submission s JOIN problem p ON s.problem_id = p.id " +
            "WHERE s.user_id = #{userId} AND s.status = #{acceptedCode} " +
            "GROUP BY p.difficulty ORDER BY p.difficulty")
    List<Map<String, Object>> ratingDistribution(@Param("userId") Long userId, @Param("acceptedCode") int acceptedCode);
}
