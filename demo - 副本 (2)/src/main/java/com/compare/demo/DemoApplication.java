package com.compare.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * 应用程序主类
 * 负责启动Spring Boot应用程序并配置组件扫描
 */
@SpringBootApplication(scanBasePackages = {
        "com.compare.controller",
        "com.compare.config",
        "com.compare.service",
        "com.compare.repository",
        "com.compare.util",
        "com.compare.entity"
})
@EnableJpaRepositories("com.compare.repository") // 启用JPA仓库
@EntityScan("com.compare.entity") // 扫描实体类
public class DemoApplication extends SpringBootServletInitializer {
    /**
     * 应用程序入口点
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(DemoApplication.class);
    }
}