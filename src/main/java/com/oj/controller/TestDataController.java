package com.oj.controller;

import com.oj.common.Result;
import com.oj.dto.TestCaseConfigRequest;
import com.oj.dto.TestCaseRequest;
import com.oj.service.TestDataService;
import com.oj.vo.TestCaseUploadVO;
import com.oj.vo.TestCaseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 测试点管理接口(仅题目创建者/管理员可用)
 */
@RestController
@RequestMapping("/api/problem/{problemId}/testcases")
@RequiredArgsConstructor
public class TestDataController {

    private final TestDataService testDataService;

    /** 测试点列表(仅元信息) */
    @GetMapping
    public Result<List<TestCaseVO>> list(@PathVariable Long problemId) {
        return Result.ok(testDataService.list(problemId));
    }

    /** 单个测试点内容(编辑表单回填) */
    @GetMapping("/{index}")
    public Result<TestCaseVO> get(@PathVariable Long problemId, @PathVariable int index) {
        return Result.ok(testDataService.get(problemId, index));
    }

    /** 追加测试点, 返回新序号 */
    @PostMapping
    public Result<Integer> add(@PathVariable Long problemId, @Valid @RequestBody TestCaseRequest request) {
        return Result.ok(testDataService.add(problemId, request));
    }

    /** 覆盖更新某个测试点 */
    @PutMapping("/{index}")
    public Result<Void> update(@PathVariable Long problemId, @PathVariable int index,
                               @Valid @RequestBody TestCaseRequest request) {
        testDataService.update(problemId, index, request);
        return Result.ok(null);
    }

    /** 行内更新测试点计分配置(分值/独立时限/内存) */
    @PutMapping("/{index}/config")
    public Result<Void> updateConfig(@PathVariable Long problemId, @PathVariable int index,
                                     @Valid @RequestBody TestCaseConfigRequest request) {
        testDataService.updateConfig(problemId, index, request);
        return Result.ok(null);
    }

    /** 删除某个测试点(后续序号前移) */
    @DeleteMapping("/{index}")
    public Result<Void> delete(@PathVariable Long problemId, @PathVariable int index) {
        testDataService.delete(problemId, index);
        return Result.ok(null);
    }

    /** 上传 zip 替换全部测试点(支持洛谷 config.yml), 返回导入数量与满分 */
    @PostMapping("/upload")
    public Result<TestCaseUploadVO> upload(@PathVariable Long problemId, @RequestParam("file") MultipartFile file) {
        return Result.ok(testDataService.replaceAll(problemId, file));
    }
}
