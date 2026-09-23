package com.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 题目标签实体, 对应表 problem_tag
 * (problem_id, tag) 为复合主键, 标签取值来自 {@link com.oj.common.ProblemTags} 白名单
 */
@Data
@TableName("problem_tag")
public class ProblemTag {

    /** 题目ID(复合主键之一, 无自增, 显式声明 IdType.INPUT 防 MP 注入 ID 逻辑) */
    @TableId(type = IdType.INPUT)
    private Long problemId;

    /** 标签(中文, 复合主键之一) */
    private String tag;
}
