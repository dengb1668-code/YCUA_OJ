package com.oj.vo;

import lombok.Data;

/**
 * 测试点视图对象: 列表接口只回传大小与计分配置, 内容接口回传全文
 */
@Data
public class TestCaseVO {

    /** 测试点序号(从 1 开始) */
    private Integer index;

    /** 输入文件字节数(仅列表接口) */
    private Long inputSize;

    /** 输出文件字节数(仅列表接口) */
    private Long outputSize;

    /** 该点分值(计分制) */
    private Integer score;

    /** 独立时间限制(毫秒), null 表示用题目默认 */
    private Integer timeLimit;

    /** 独立内存限制(MB), null 表示用题目默认 */
    private Integer memoryLimit;

    /** 输入内容(仅内容接口) */
    private String input;

    /** 期望输出内容(仅内容接口) */
    private String output;

    public static TestCaseVO summary(int index, long inputSize, long outputSize,
                                     int score, Integer timeLimit, Integer memoryLimit) {
        TestCaseVO vo = new TestCaseVO();
        vo.setIndex(index);
        vo.setInputSize(inputSize);
        vo.setOutputSize(outputSize);
        vo.setScore(score);
        vo.setTimeLimit(timeLimit);
        vo.setMemoryLimit(memoryLimit);
        return vo;
    }

    public static TestCaseVO content(int index, String input, String output,
                                     int score, Integer timeLimit, Integer memoryLimit) {
        TestCaseVO vo = new TestCaseVO();
        vo.setIndex(index);
        vo.setInput(input);
        vo.setOutput(output);
        vo.setScore(score);
        vo.setTimeLimit(timeLimit);
        vo.setMemoryLimit(memoryLimit);
        return vo;
    }
}
