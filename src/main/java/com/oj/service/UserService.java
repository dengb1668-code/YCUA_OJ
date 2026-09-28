package com.oj.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.oj.dto.CertApplyRequest;
import com.oj.dto.CertReviewRequest;
import com.oj.dto.ForgotPasswordRequest;
import com.oj.dto.LoginRequest;
import com.oj.dto.RegisterRequest;
import com.oj.entity.User;
import com.oj.enums.CertStatus;
import com.oj.vo.CertAdminVO;
import com.oj.vo.CertVO;
import com.oj.vo.LoginVO;
import com.oj.vo.UserAdminVO;

public interface UserService extends IService<User> {

    /**
     * 注册(成功后自动登录, 直接返回令牌)
     */
    LoginVO register(RegisterRequest request);

    /**
     * 登录, 用户名或密码错误时抛 IllegalArgumentException
     */
    LoginVO login(LoginRequest request);

    /**
     * 用户管理分页(仅站长), keyword 模糊匹配用户名/昵称
     */
    Page<UserAdminVO> pageUsers(String keyword, long pageNum, long pageSize);

    /**
     * 修改用户角色(仅站长, 只允许设为普通用户/管理员, 站长不可被修改)
     */
    void updateRole(Long userId, Integer role);

    /**
     * 找回密码: 验证图形验证码 + 用户名与手机号匹配后重置密码(无短信验证)
     */
    void forgotPassword(ForgotPasswordRequest request);

    /**
     * 学生认证申请(普通用户): 审核中/已通过时不可重复申请, 已驳回可重新申请
     */
    void applyCert(CertApplyRequest request);

    /**
     * 我的学生认证信息
     */
    CertVO myCert();

    /**
     * 学生认证申请分页(负责人/站长审核列表)
     */
    Page<CertAdminVO> pageCert(CertStatus status, long pageNum, long pageSize);

    /**
     * 学生认证审核(负责人/站长): 通过或驳回
     */
    void reviewCert(CertReviewRequest request);
}
