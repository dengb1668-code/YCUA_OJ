package com.oj.vo;

import lombok.Data;

/**
 * 加入比赛成功响应: 比赛访问 token
 */
@Data
public class ContestJoinVO {

    private Long contestId;

    /** 比赛访问 token(用于题目/榜单/提交接口的 X-Contest-Token 头) */
    private String token;
}
