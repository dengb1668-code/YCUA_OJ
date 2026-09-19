package com.oj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OJ 系统启动类
 * (Mapper 接口已使用 @Mapper 注解, 无需 @MapperScan)
 */
@SpringBootApplication
public class OjApplication {

    public static void main(String[] args) {
        SpringApplication.run(OjApplication.class, args);
    }
}
