package com.oj.judge;

import com.oj.dto.CustomTestRequest;
import com.oj.vo.CustomTestVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * 自定义测试服务: 在本机真实编译/运行用户代码(AtCoder コードテスト 对应功能)
 * 编译运行细节由 CodeRunner 提供(与真实判题共用)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomTestService {

    /** 自测最长运行时间(毫秒) */
    private static final long TIMEOUT_MILLIS = 5000;

    private final CodeRunner codeRunner;

    public CustomTestVO run(CustomTestRequest request) {
        // 检查本机工具链
        String toolError = codeRunner.checkToolchain(request.getLanguage());
        if (toolError != null) {
            return CustomTestVO.error(toolError);
        }

        Path dir = null;
        try {
            dir = codeRunner.createTempDir();
            codeRunner.writeSource(dir, request.getLanguage(), request.getCode());

            String compileError = codeRunner.compile(dir, request.getLanguage());
            if (compileError != null) {
                return CustomTestVO.error("编译失败:\n" + compileError);
            }

            CodeRunner.RunResult result = codeRunner.run(dir, request.getLanguage(), request.getInput(), TIMEOUT_MILLIS);
            if (result.timedOut()) {
                return CustomTestVO.error("运行超时(>5 秒), 已强制终止");
            }

            CustomTestVO vo = new CustomTestVO();
            vo.setOutput(result.stdout());
            if (result.exitCode() != 0 || !result.stderr().isEmpty()) {
                vo.setError(result.stderr().isEmpty()
                        ? "程序异常退出, exitCode=" + result.exitCode()
                        : result.stderr());
            }
            return vo;
        } catch (Exception e) {
            log.warn("自测执行失败", e);
            return CustomTestVO.error("自测执行失败: " + e.getMessage());
        } finally {
            codeRunner.deleteQuietly(dir);
        }
    }
}
