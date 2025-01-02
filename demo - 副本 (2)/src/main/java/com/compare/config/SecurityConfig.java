package com.compare.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring Security配置类
 * 用于配置应用程序的安全性设置，包括认证和授权规则
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * 配置HTTP安全规则
     * 
     * @param http HTTP安全配置对象
     * @throws Exception 配置过程中可能发生的异常
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .antMatchers("/admin/**", "/admin/index/**").hasAuthority("ROLE_ADMIN")
                .antMatchers("/user/**", "/user/upload/**", "/user/compare/**", "/user/report/**",
                        "/user/change-password")
                .hasAnyAuthority("ROLE_ADMIN", "ROLE_USER")
                .antMatchers("/login", "/register").permitAll()
                .anyRequest().authenticated()
                .and()
                // 配置登录功能
                .formLogin()
                .loginPage("/login")
                .successHandler((request, response, authentication) -> {
                    if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                        response.sendRedirect("/admin/index");
                    } else {
                        response.sendRedirect("/user");
                    }
                })
                .permitAll()
                .and()
                // 配置登出功能
                .logout()
                .permitAll()
                .and()
                .csrf().disable()
                .headers().frameOptions().disable();
    }

    /**
     * 配置密码编码器
     * 
     * @return BCrypt密码编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置用户详情服务
     * 
     * @return 内存中的用户详情管理器
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }
}
