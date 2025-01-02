package com.compare.controller;

import com.compare.entity.User;
import com.compare.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

/**
 * 用户注册控制器
 * 处理用户注册相关的请求
 */
@Controller
public class RegisterController {

    /**
     * 用户数据访问接口
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * 密码加密工具
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 显示注册页面
     * 
     * @return 返回register视图
     */
    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    /**
     * 处理用户注册请求
     * 
     * @param username        用户名
     * @param password        密码
     * @param confirmPassword 确认密码
     * @return 注册成功后重定向到登录页面，失败则返回注册页面并显示错误信息
     */
    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword) {
        try {
            // 验证密码是否一致
            if (!password.equals(confirmPassword)) {
                return "redirect:/register?error=password";
            }

            // 检查用户名是否已存在
            if (userRepository.findByUsername(username).isPresent()) {
                return "redirect:/register?error=exists";
            }

            // 创建新用户对象并设置属性
            User user = new User();
            user.setUsername(username);
            // 对密码进行加密存储
            user.setPassword(passwordEncoder.encode(password));
            // 设置用户角色为普通用户
            user.setRole("ROLE_USER");
            // 记录用户创建时间
            user.setCreateTime(LocalDateTime.now());
            // 保存原始密码（注意：实际生产环境中不建议这样做）
            user.setOriginalPassword(password);
            // 保存用户信息到数据库
            userRepository.save(user);

            // 注册成功，重定向到登录页面
            return "redirect:/login?success=register";
        } catch (Exception e) {
            // 发生异常时返回注册页面并显示错误信息
            return "redirect:/register?error=unknown";
        }
    }
}