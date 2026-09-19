package com.oj;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * 评测机进程入口: 不启动 Web 服务器, 只消费 Redis 判题队列
 * <p>
 * 本地运行:
 * mvn spring-boot:run -Dspring-boot.run.main-class=com.oj.JudgeApplication \
 *     -Dspring-boot.run.arguments=--spring.profiles.active=judge
 * <p>
 * 部署时 Web 与评测机分别部署到不同机器/容器, 共享 MySQL 与 Redis,
 * 评测机水平扩容即可提升并发判题能力
 */
@SpringBootApplication
public class JudgeApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(JudgeApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
