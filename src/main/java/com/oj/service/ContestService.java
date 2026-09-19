package com.oj.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.oj.dto.ContestCreateRequest;
import com.oj.dto.ContestJoinRequest;
import com.oj.dto.ContestProblemUpdateRequest;
import com.oj.dto.ContestUpdateRequest;
import com.oj.entity.Contest;
import com.oj.vo.ContestDetailVO;
import com.oj.vo.ContestJoinVO;
import com.oj.vo.ContestListVO;
import com.oj.vo.ContestProblemVO;
import com.oj.vo.StandingsVO;

import java.util.List;

public interface ContestService extends IService<Contest> {

    /**
     * 创建比赛(任何登录用户), 返回比赛ID
     */
    Long create(ContestCreateRequest request);

    /**
     * 比赛分页列表(按创建时间倒序)
     */
    Page<ContestListVO> page(long pageNum, long pageSize);

    /**
     * 比赛详情: 基本信息无需 token; 未开始且非创建者/管理端时不返回题目列表
     */
    ContestDetailVO detail(Long id, String contestToken);

    /**
     * 加入比赛(密码制需密码), 返回访问 token
     */
    ContestJoinVO join(Long id, ContestJoinRequest request);

    /**
     * 编辑比赛(创建者或管理端; 开赛或已有提交后赛制/开始时间锁定, 结束时间只可延长)
     */
    void update(Long id, ContestUpdateRequest request);

    /**
     * 更新比赛题目列表(创建者或管理端, 仅未开赛可改; 顺序即 A/B/C 题号)
     */
    void updateProblems(Long id, ContestProblemUpdateRequest request);

    /**
     * 删除比赛(创建者或管理端; 比赛提交记录保留)
     */
    void delete(Long id);

    /**
     * 比赛题目列表(需比赛访问 token; 未开赛且非创建者/管理端拒绝)
     */
    List<ContestProblemVO> problems(Long id, String contestToken);

    /**
     * 榜单: ICPC 实时; OI 赛期非创建者/管理端隐藏, 结束后公开; IOI 实时
     */
    StandingsVO standings(Long id, String contestToken);

    /**
     * 校验比赛内提交合法性: 比赛存在、时间窗内、访问 token 有效、题目在比赛题目集中
     */
    void checkSubmitAccess(Long contestId, Long problemId, String contestToken);
}
