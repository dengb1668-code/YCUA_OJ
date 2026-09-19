package com.oj.judge;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 判题任务队列(Redis List):
 * Web 端 leftPush 入队, 评测机 BRPOP 阻塞消费
 */
@Component
@RequiredArgsConstructor
public class JudgeQueue {

    public static final String QUEUE_KEY = "oj:judge:queue";

    private final StringRedisTemplate redisTemplate;

    /** 提交入队(Web 端调用) */
    public void push(Long submissionId) {
        redisTemplate.opsForList().leftPush(QUEUE_KEY, String.valueOf(submissionId));
    }

    /** 阻塞取任务, 队列为空时最多等 5 秒返回 null(评测机主循环调用) */
    public Long blockingPop() {
        String value = redisTemplate.opsForList().rightPop(QUEUE_KEY, 5, TimeUnit.SECONDS);
        return value == null ? null : Long.valueOf(value);
    }
}
