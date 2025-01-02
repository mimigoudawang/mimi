package com.compare.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 用于存储系统用户信息
 */
@Entity
@Table(name = "users")
public class User {
    /**
     * 用户ID，主键，自动生成
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名，唯一且不能为空
     */
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * 加密后的密码
     */
    @Column(nullable = false)
    private String password;

    /**
     * 用户角色，用于权限控制
     */
    @Column(nullable = false)
    private String role;

    /**
     * 用户创建时间
     */
    @Column(nullable = false)
    private LocalDateTime createTime;

    /**
     * 用户原始密码（明文）
     * 注意：存储明文密码可能存在安全风险
     */
    @Column(name = "original_password")
    private String originalPassword;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getOriginalPassword() {
        return originalPassword;
    }

    public void setOriginalPassword(String originalPassword) {
        this.originalPassword = originalPassword;
    }
}