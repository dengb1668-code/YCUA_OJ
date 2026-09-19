package com.oj.dto;

import lombok.Data;

/**
 * 加入比赛请求(密码制比赛需要)
 */
@Data
public class ContestJoinRequest {

    private String password;
}
