package com.compare.service;

import com.compare.entity.User;
import com.compare.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Spring Security用户详情服务实现类
 * 用于处理用户认证和授权相关的业务逻辑
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * 根据用户名加载用户详情
     * 实现Spring Security的用户认证逻辑
     *
     * @param username 用户名
     * @return UserDetails对象，包含用户认证和授权信息
     * @throws UsernameNotFoundException 当用户不存在时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("正在验证用户: " + username);
        try {
            // 从数据库查找用户
            User user = userRepository.findByUsernameCustom(username)
                    .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

            // 输出调试信息
            System.out.println("找到用户: " + username);
            System.out.println("用户角色: " + user.getRole());
            System.out.println("密码hash: " + user.getPassword());

            // 创建并返回Spring Security的UserDetails对象
            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    Collections.singletonList(new SimpleGrantedAuthority(user.getRole())));
        } catch (Exception e) {
            // 异常处理和日志记录
            System.out.println("验证用户时发生错误: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}