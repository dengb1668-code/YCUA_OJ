package com.oj.common;

import java.util.List;
import java.util.Set;

/**
 * 题目标签白名单(Codeforces 标签集翻译为中文的固定集合)
 * 出题时从该集合多选, 列表按标签筛选; 校验用 SET 白名单过滤非法值
 */
public final class ProblemTags {

    public static final List<String> ALL = List.of(
            "动态规划", "贪心", "图论", "数学", "数据结构", "字符串",
            "二分", "搜索", "构造", "模拟", "枚举", "前缀和",
            "双指针", "树", "最短路", "并查集", "哈希", "线段树",
            "数论", "组合数学", "位运算", "博弈论", "计算几何", "交互",
            "排序", "概率", "矩阵", "分治", "高精度", "递归");

    public static final Set<String> SET = Set.copyOf(ALL);

    /** 单个标签是否合法 */
    public static boolean isValid(String tag) {
        return tag != null && SET.contains(tag);
    }

    /** 过滤出合法标签并去重(保持 ALL 中的顺序) */
    public static List<String> filterValid(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        return ALL.stream().filter(tags::contains).toList();
    }

    private ProblemTags() {
    }
}
