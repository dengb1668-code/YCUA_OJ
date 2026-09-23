package com.oj.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步任务配置: 开启 @Async 并定义判题专用线程池
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 判题线程池, 并发由配置控制(默认 1, 适配 2C2G 小服务器;
     * 编译占大头(单次 C++ 编译约 2-5 秒), 高配机器可调大)
     */
    @Bean("judgeExecutor")
    public ThreadPoolTaskExecutor judgeExecutor(
            @Value("${oj.judge.core-pool-size:1}") int corePoolSize,
            @Value("${oj.judge.max-pool-size:1}") int maxPoolSize,
            @Value("${oj.judge.queue-capacity:200}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("judge-");
        executor.initialize();
        return executor;
    }
}
