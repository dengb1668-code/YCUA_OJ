package com.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oj.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 登录用: 显式查出 password 字段
     * (User.password 标了 select=false, 普通查询不会返回, 需手动指定)
     */
    @Select("SELECT id, username, password, nickname, email, avatar, role FROM `user` WHERE username = #{username}")
    User selectForLogin(String username);
}
