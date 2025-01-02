package com.compare.config;

import com.compare.entity.User;
import com.compare.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器
 * 在应用启动时自动创建管理员账户
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 应用启动时执行的初始化方法
     * 检查并创建管理员账户
     */
    @Override
    public void run(String... args) {
        // 如果admin用户不存在，则创建
        if (userRepository.findByUsername("admin") == null) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin")); // 加密密码
            admin.setRole("ROLE_ADMIN");
            userRepository.save(admin);
        }
    }
}