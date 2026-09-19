package com.oj.service;

import com.oj.common.UserContext;
import com.oj.dto.TestCaseConfigRequest;
import com.oj.dto.TestCaseRequest;
import com.oj.entity.Problem;
import com.oj.judge.TestDataStore;
import com.oj.mapper.ProblemMapper;
import com.oj.vo.TestCaseUploadVO;
import com.oj.vo.TestCaseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 测试点管理服务: 权限校验 + 按题目串行化文件操作(Web 端管理 API 用)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestDataService {

    private final TestDataStore testDataStore;
    private final ProblemMapper problemMapper;
    private final PermissionService permissionService;

    /** 每题的互斥锁: 防止同一题目的并发增删改/上传互相穿插 */
    private final ConcurrentHashMap<Long, Object> locks = new ConcurrentHashMap<>();

    /** 带受检异常的存储操作 */
    private interface StoreOp<T> {
        T run() throws IOException;
    }

    /**
     * 是否可管理某题的测试点: 创建者本人 ∨ 管理端(管理员/站长)。
     * 旧题(author_id 为空)只有管理端可管理。
     */
    public boolean canManage(Problem problem, Long userId) {
        if (problem.getAuthorId() != null && problem.getAuthorId().equals(userId)) {
            return true;
        }
        return permissionService.isManager(userId);
    }

    /** 校验当前用户可管理该题, 否则抛异常 */
    public void requireManage(Long problemId) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new IllegalArgumentException("题目不存在: id=" + problemId);
        }
        if (!canManage(problem, UserContext.getUserId())) {
            throw new IllegalArgumentException("无权限管理该题目的测试点");
        }
    }

    public List<TestCaseVO> list(Long problemId) {
        requireManage(problemId);
        return withLock(problemId, () -> testDataStore.listInfo(problemId).stream()
                .map(info -> TestCaseVO.summary(info.index(), info.inputSize(), info.outputSize(),
                        info.score(), info.timeLimit(), info.memoryLimit()))
                .toList());
    }

    public TestCaseVO get(Long problemId, int index) {
        requireManage(problemId);
        return withLock(problemId, () -> {
            TestDataStore.CaseData data = testDataStore.load(problemId, index);
            return TestCaseVO.content(data.index(), data.input(), data.output(),
                    data.score(), data.timeLimit(), data.memoryLimit());
        });
    }

    public int add(Long problemId, TestCaseRequest request) {
        requireManage(problemId);
        return withLock(problemId, () -> testDataStore.addCase(problemId, request.getInput(), request.getOutput(),
                request.getScore(), request.getTimeLimit(), request.getMemoryLimit()));
    }

    public void update(Long problemId, int index, TestCaseRequest request) {
        requireManage(problemId);
        withLock(problemId, () -> {
            testDataStore.updateCase(problemId, index, request.getInput(), request.getOutput());
            return null;
        });
    }

    public void updateConfig(Long problemId, int index, TestCaseConfigRequest request) {
        requireManage(problemId);
        withLock(problemId, () -> {
            testDataStore.updateConfig(problemId, index, request.getScore(), request.getTimeLimit(), request.getMemoryLimit());
            return null;
        });
    }

    public void delete(Long problemId, int index) {
        requireManage(problemId);
        withLock(problemId, () -> {
            testDataStore.deleteCase(problemId, index);
            return null;
        });
    }

    public TestCaseUploadVO replaceAll(Long problemId, MultipartFile zip) {
        requireManage(problemId);
        return withLock(problemId, () -> {
            int count = testDataStore.replaceAllFromZip(problemId, zip);
            int totalScore = testDataStore.totalScore(problemId);
            return new TestCaseUploadVO(count, totalScore);
        });
    }

    /** 测试点数量(详情页展示用, IO 失败按 0 处理) */
    public int count(Long problemId) {
        try {
            return testDataStore.listIndexes(problemId).size();
        } catch (IOException e) {
            log.warn("统计测试点数量失败: problemId={}", problemId, e);
            return 0;
        }
    }

    private <T> T withLock(Long problemId, StoreOp<T> op) {
        synchronized (locks.computeIfAbsent(problemId, k -> new Object())) {
            try {
                return op.run();
            } catch (IOException e) {
                throw new IllegalStateException("测试点文件操作失败: " + e.getMessage(), e);
            }
        }
    }
}
