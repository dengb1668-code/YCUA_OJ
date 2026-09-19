package com.oj.judge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 评测机主循环: 阻塞拉取队列任务, 交给 JudgeService 判题
 * 仅评测机进程加载(profile=judge), Web 进程不启动消费循环
 */
@Slf4j
@Component
@Profile("judge")
@RequiredArgsConstructor
public class JudgeWorker implements CommandLineRunner {

    private final JudgeQueue judgeQueue;
    private final JudgeService judgeService;

    @Override
    public void run(String... args) {
        Thread thread = new Thread(this::runLoop, "judge-worker-main");
        thread.setDaemon(false);
        thread.start();
        log.info("评测机已启动, 开始监听判题队列: {}", JudgeQueue.QUEUE_KEY);
    }

    private void runLoop() {
        while (true) {
            try {
                Long submissionId = judgeQueue.blockingPop();
                if (submissionId != null) {
                    log.info("领取判题任务: submissionId={}", submissionId);
                    // @Async 提交到判题线程池(AsyncConfig#judgeExecutor)
                    judgeService.judge(submissionId);
                }
            } catch (Exception e) {
                log.error("判题队列消费异常", e);
            }
        }
    }
}
