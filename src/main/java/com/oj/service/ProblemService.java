package com.oj.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.oj.dto.ProblemCreateRequest;
import com.oj.entity.Problem;
import com.oj.vo.ProblemListVO;

public interface ProblemService extends IService<Problem> {

    /**
     * 分页查询题目列表(不含 description/samples 大字段)
     *
     * @param userId 当前用户ID, 用于聚合每题做题状态
     */
    Page<ProblemListVO> pageProblems(long pageNum, long pageSize, String keyword, Long userId);

    /**
     * 查询单题详情, 不存在时抛出 IllegalArgumentException
     */
    Problem getProblemDetail(Long id);

    /**
     * 创建题目
     *
     * @return 新题目ID
     */
    Long createProblem(ProblemCreateRequest request);

    /**
     * 编辑题目题面(仅创建者/管理员/旧题宽松), 无权限或不存在时抛出 IllegalArgumentException
     */
    void updateProblem(Long id, ProblemCreateRequest request);

    /**
     * 删除题目(创建者或管理端; 被比赛引用的题目不可删):
     * 级联删除提交记录、帖子与回复、测试点文件
     */
    void deleteProblem(Long id);
}
