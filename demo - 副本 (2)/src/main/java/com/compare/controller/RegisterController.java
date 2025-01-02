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

@Controller
public class RegisterController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

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

            // 创建新用户
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole("ROLE_USER");
            user.setCreateTime(LocalDateTime.now());
            user.setOriginalPassword(password);
            userRepository.save(user);

            return "redirect:/login?success=register";
        } catch (Exception e) {
            return "redirect:/register?error=unknown";
        }
    }
}