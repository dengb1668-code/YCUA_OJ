package com.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * zip 上传测试点结果: 导入数量 + 上传后的题目满分
 */
@Data
@AllArgsConstructor
public class TestCaseUploadVO {

    /** 导入的测试点数量 */
    private Integer count;

    /** 上传后的题目满分(各测试点分值总和) */
    private Integer totalScore;
}
