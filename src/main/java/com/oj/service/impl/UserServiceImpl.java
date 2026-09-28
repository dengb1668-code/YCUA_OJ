package com.oj.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.oj.common.CaptchaService;
import com.oj.common.JwtUtil;
import com.oj.common.UserContext;
import com.oj.dto.CertApplyRequest;
import com.oj.dto.CertReviewRequest;
import com.oj.dto.ForgotPasswordRequest;
import com.oj.dto.LoginRequest;
import com.oj.dto.RegisterRequest;
import com.oj.entity.User;
import com.oj.enums.CertStatus;
import com.oj.enums.UserRole;
import com.oj.mapper.UserMapper;
import com.oj.service.PermissionService;
import com.oj.service.UserService;
import com.oj.vo.CertAdminVO;
import com.oj.vo.CertVO;
import com.oj.vo.LoginVO;
import com.oj.vo.UserAdminVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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
        user.setCertStatus(CertStatus.NONE);
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
        permissionService.requireManager();
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
        // 集训队负责人/站长可管理用户; 只能设为学生或负责人, 站长角色不可动
        permissionService.requireManager();
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

    @Override
    public void applyCert(CertApplyRequest request) {
        User user = getById(UserContext.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (user.getRole() != UserRole.USER) {
            throw new IllegalArgumentException("负责人/站长无需学生认证");
        }
        if (user.getCertStatus() == CertStatus.PENDING) {
            throw new IllegalArgumentException("认证申请审核中, 请耐心等待");
        }
        if (user.getCertStatus() == CertStatus.APPROVED) {
            throw new IllegalArgumentException("已完成学生认证, 无需重复申请");
        }
        // NONE/REJECTED 均可申请(驳回后可修改信息重新提交)
        // 用显式 UpdateWrapper: updateById 默认忽略 null, 清不掉旧的审核信息
        baseMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getRealName, request.getRealName().trim())
                .set(User::getGrade, request.getGrade().trim())
                .set(User::getMajor, request.getMajor().trim())
                .set(User::getCertStatus, CertStatus.PENDING)
                .set(User::getCertApplyTime, LocalDateTime.now())
                .set(User::getCertReviewTime, null)
                .set(User::getCertReviewerId, null)
                .set(User::getCertRejectReason, null));
    }

    @Override
    public CertVO myCert() {
        User user = getById(UserContext.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        CertVO vo = new CertVO();
        vo.setCertStatus(user.getRole() == UserRole.USER ? user.getCertStatus() : CertStatus.APPROVED);
        if (user.getRole() != UserRole.USER) {
            return vo;
        }
        // realName/grade/major 为 select=false 字段, 显式查询
        User detail = baseMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getId, user.getId())
                .select(User::getRealName, User::getGrade, User::getMajor,
                        User::getCertStatus, User::getCertApplyTime, User::getCertReviewTime, User::getCertRejectReason));
        vo.setRealName(detail.getRealName());
        vo.setGrade(detail.getGrade());
        vo.setMajor(detail.getMajor());
        vo.setCertStatus(detail.getCertStatus());
        vo.setCertApplyTime(detail.getCertApplyTime());
        vo.setCertReviewTime(detail.getCertReviewTime());
        vo.setCertRejectReason(detail.getCertRejectReason());
        return vo;
    }

    @Override
    public Page<CertAdminVO> pageCert(CertStatus status, long pageNum, long pageSize) {
        permissionService.requireManager();
        Page<User> page = lambdaQuery()
                .eq(status != null, User::getCertStatus, status)
                .and(w -> w.isNotNull(User::getCertApplyTime))
                .orderByAsc(User::getCertStatus)
                .orderByDesc(User::getCertApplyTime)
                .page(new Page<>(pageNum, pageSize));
        // realName/grade/major 为 select=false 字段, 需要按 id 批量显式查询
        java.util.List<Long> ids = page.getRecords().stream().map(User::getId).collect(Collectors.toList());
        java.util.Map<Long, User> detailMap = ids.isEmpty() ? java.util.Map.of()
                : baseMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .in(User::getId, ids)
                        .select(User::getId, User::getRealName, User::getGrade, User::getMajor))
                        .stream().collect(Collectors.toMap(User::getId, u -> u));
        Page<CertAdminVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(u -> {
            CertAdminVO vo = new CertAdminVO();
            vo.setUserId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            vo.setCertStatus(u.getCertStatus());
            vo.setCertApplyTime(u.getCertApplyTime());
            User d = detailMap.get(u.getId());
            if (d != null) {
                vo.setRealName(d.getRealName());
                vo.setGrade(d.getGrade());
                vo.setMajor(d.getMajor());
            }
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public void reviewCert(CertReviewRequest request) {
        permissionService.requireManager();
        User target = getById(request.getUserId());
        if (target == null) {
            throw new IllegalArgumentException("用户不存在: id=" + request.getUserId());
        }
        if (target.getRole() != UserRole.USER) {
            throw new IllegalArgumentException("该用户无需学生认证");
        }
        if (target.getCertStatus() != CertStatus.PENDING) {
            throw new IllegalArgumentException("该申请不在待审核状态");
        }
        // 显式 UpdateWrapper: 通过时需把驳回原因置 NULL
        baseMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                .eq(User::getId, target.getId())
                .set(User::getCertStatus, request.getApprove() ? CertStatus.APPROVED : CertStatus.REJECTED)
                .set(User::getCertReviewTime, LocalDateTime.now())
                .set(User::getCertReviewerId, UserContext.getUserId())
                .set(User::getCertRejectReason, request.getApprove() ? null : request.getReason()));
    }

    private LoginVO buildLoginVO(User user) {
        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.generateToken(user.getId(), user.getUsername()));
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setCertStatus(user.getCertStatus());
        return vo;
    }
}
