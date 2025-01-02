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

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("正在验证用户: " + username);
        try {
            User user = userRepository.findByUsernameCustom(username)
                    .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

            System.out.println("找到用户: " + username);
            System.out.println("用户角色: " + user.getRole());
            System.out.println("密码hash: " + user.getPassword());

            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    Collections.singletonList(new SimpleGrantedAuthority(user.getRole())));
        } catch (Exception e) {
            System.out.println("验证用户时发生错误: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}