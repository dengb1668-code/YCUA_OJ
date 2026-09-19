package com.oj.config;

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
     * 判题线程池(模拟判题使用, 后续接入真实沙箱时可调整参数)
     */
    @Bean("judgeExecutor")
    public ThreadPoolTaskExecutor judgeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 编译占大头(本机单次 C++ 编译约 2-5 秒), 4 个并发减少排队等待
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("judge-");
        executor.initialize();
        return executor;
    }
}
