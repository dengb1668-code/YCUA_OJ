package com.oj.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.oj.enums.JudgeMode;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目实体, 对应表 problem
 */
@Data
@TableName(value = "problem", autoResultMap = true)
public class Problem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题目标题 */
    private String title;

    /** 题目来源(如: 洛谷 P1001) */
    private String source;

    /** 题目描述(Markdown) */
    private String description;

    /** 输入格式说明 */
    private String inputDescription;

    /** 输出格式说明 */
    private String outputDescription;

    /**
     * 样例列表, 数据库中存 JSON 数组,
     * 通过 JacksonTypeHandler 自动与 List&lt;Sample&gt; 互转
     * (需 @TableName 上开启 autoResultMap = true)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Sample> samples;

    /** 时间限制(毫秒) */
    private Integer timeLimit;

    /** 内存限制(MB) */
    private Integer memoryLimit;

    /** 难度(Codeforces Rating, 800-3500) */
    private Integer difficulty;

    /** 判题模式: ICPC(首错即停, 无部分分) / IOI(逐点部分分) */
    private JudgeMode judgeMode;

    /** 创建者用户ID(测试点管理权限依据) */
    private Long authorId;

    /** 题目标签(非表字段, 存 problem_tag 表, 列表/详情接口批量填充) */
    @TableField(exist = false)
    private List<String> tags;

    /** 当前登录用户是否可管理本题测试点(非表字段, 详情接口填充) */
    @TableField(exist = false)
    private Boolean canManage;

    /** 测试点数量(非表字段, 详情接口填充, 无测试点时判题回退样例) */
    @TableField(exist = false)
    private Integer testCaseCount;

    /** 创建时间(插入时自动填充) */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间(插入/更新时自动填充) */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 单个样例: 输入 + 输出 + 可选的样例解释
     */
    @Data
    public static class Sample {
        private String input;
        private String output;
        /** 样例解释(可选, Markdown) */
        private String explanation;
    }
}
