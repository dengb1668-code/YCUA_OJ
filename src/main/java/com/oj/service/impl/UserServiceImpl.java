package com.oj.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.oj.common.CaptchaService;
import com.oj.common.JwtUtil;
import com.oj.dto.ForgotPasswordRequest;
import com.oj.dto.LoginRequest;
import com.oj.dto.RegisterRequest;
import com.oj.entity.User;
import com.oj.enums.UserRole;
import com.oj.mapper.UserMapper;
import com.oj.service.PermissionService;
import com.oj.service.UserService;
import com.oj.vo.LoginVO;
import com.oj.vo.UserAdminVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final PermissionService permissionService;

    @Override
    public LoginVO register(RegisterRequest request) {
        if (lambdaQuery().eq(User::getUsername, request.getUsername()).count() > 0) {
            throw new IllegalArgumentException("用户名已被注册");
        }
        if (lambdaQuery().eq(User::getPhone, request.getPhone()).count() > 0) {
            throw new IllegalArgumentException("手机号已被注册");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        // 注册只收用户名/密码/手机号, 昵称默认同用户名
        user.setNickname(request.getUsername());
        user.setRole(UserRole.USER);
        save(user);
        return buildLoginVO(user);
    }

    @Override
    public LoginVO login(LoginRequest request) {
        // 先验图片验证码(一次性, 防爆破), 再验用户名密码
        captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());
        User user = baseMapper.selectForLogin(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return buildLoginVO(user);
    }

    @Override
    public Page<UserAdminVO> pageUsers(String keyword, long pageNum, long pageSize) {
        permissionService.requireOwner();
        Page<User> page = lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(User::getUsername, keyword)
                        .or()
                        .like(User::getNickname, keyword))
                .orderByAsc(User::getId)
                .page(new Page<>(pageNum, pageSize));
        // 3.5.17 中 Page.convert 返回 IPage 而非 Page, 手动构造以保持返回类型
        Page<UserAdminVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(UserAdminVO::from)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public void updateRole(Long userId, Integer role) {
        permissionService.requireOwner();
        User target = getById(userId);
        if (target == null) {
            throw new IllegalArgumentException("用户不存在: id=" + userId);
        }
        if (target.getRole() == UserRole.OWNER) {
            throw new IllegalArgumentException("不能修改站长的角色");
        }
        target.setRole(role == UserRole.ADMIN.getCode() ? UserRole.ADMIN : UserRole.USER);
        updateById(target);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        // 先验图形验证码(一次性), 再匹配用户名与手机号, 全部通过才重置
        captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());
        User user = lambdaQuery().eq(User::getUsername, request.getUsername()).one();
        if (user == null || !request.getPhone().equals(user.getPhone())) {
            // 统一报错, 不暴露账号是否存在
            throw new IllegalArgumentException("用户名与手机号不匹配");
        }
        User update = new User();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(request.getNewPassword()));
        updateById(update);
    }

    private LoginVO buildLoginVO(User user) {
        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.generateToken(user.getId(), user.getUsername()));
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        return vo;
    }
}
