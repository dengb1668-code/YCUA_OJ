package com.oj.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.oj.enums.JudgeStatus;
import com.oj.enums.Language;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提交记录实体, 对应表 submission
 */
@Data
@TableName(value = "submission", autoResultMap = true)
public class Submission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交用户ID */
    private Long userId;

    /** 题目ID */
    private Long problemId;

    /** 所属比赛ID(NULL=非比赛提交) */
    private Long contestId;

    /** 编程语言 */
    private Language language;

    /** 提交的源代码 */
    private String code;

    /** 判题状态 */
    private JudgeStatus status;

    /** 得分(计分制, 满分=题目测试点分值总和); 样例回退模式为 100 或 0 */
    private Integer score;

    /** 运行耗时(毫秒), 判题完成后写入 */
    private Integer timeUsed;

    /** 运行内存(KB), 判题完成后写入 */
    private Integer memoryUsed;

    /** 错误信息(编译错误/运行时错误输出) */
    private String errorMessage;

    /**
     * 逐样例判题结果, 数据库存 JSON 数组,
     * 通过 JacksonTypeHandler 自动与 List&lt;JudgeCase&gt; 互转
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<JudgeCase> judgeDetail;

    /** 提交时间(插入时自动填充) */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 判题完成时间(插入/更新时自动填充) */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 单个用例的判题结果
     */
    @Data
    public static class JudgeCase {
        private String caseName;
        private JudgeStatus status;
        private Integer timeUsed;
        private Integer memoryUsed;
        /** 该用例得分(计分制; 样例回退模式为 null) */
        private Integer score;
        /** 该用例满分(计分制; 样例回退模式为 null) */
        private Integer fullScore;
    }
}
