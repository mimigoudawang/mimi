package com.compare.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户文档实体类
 * 用于存储用户上传的文档信息
 */
@Entity
@Table(name = "user_documents")
public class UserDocument {
    /**
     * 文档ID，主键，自动生成
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文档文件名
     */
    @Column(nullable = false)
    private String fileName;

    /**
     * 文档上传时间
     */
    @Column(nullable = false)
    private LocalDateTime uploadTime;

    /**
     * 文档所属用户
     * 多对一关系：多个文档可以属于同一个用户
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 是否已生成查重报告
     */
    private boolean hasReport;

    /**
     * 关联的查重报告ID
     */
    private Long reportId;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isHasReport() {
        return hasReport;
    }

    public void setHasReport(boolean hasReport) {
        this.hasReport = hasReport;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }
}