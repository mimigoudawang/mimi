package com.compare;

import com.compare.demo.DemoApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Servlet初始化器
 * 用于支持WAR包部署到外部容器
 * 继承SpringBootServletInitializer以支持Spring Boot应用程序的WAR部署
 */
public class ServletInitializer extends SpringBootServletInitializer {

    /**
     * 配置Spring应用程序构建器
     * 
     * @param application Spring应用程序构建器
     * @return 配置后的构建器
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(DemoApplication.class);
    }
}