package com.compare.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 文档实体类
 * 用于存储文档库中的文档信息
 */
@Entity
@Table(name = "documents")
public class Document {

    /**
     * 文档ID，主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文件名
     */
    @Column(nullable = false)
    private String fileName;

    /**
     * 文档标题
     */
    @Column
    private String title;

    /**
     * 文档作者
     */
    @Column
    private String author;

    /**
     * 文档类型（例如：论文、报告、文章等）
     */
    @Column
    private String documentType;

    /**
     * 文档内容
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 文档的关键词列表
     */
    @Column(columnDefinition = "TEXT")
    private String keywords;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 文档的��词结果缓存
     * 用于提高比对效率
     */
    @Column(columnDefinition = "TEXT")
    private String tokenizedContent;

    /**
     * 文档描述
     */
    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean isTemplate = false;

    @ManyToOne
    @JoinColumn(name = "uploader_id")
    private User uploader;

    @Transient
    private boolean hasReport;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getTokenizedContent() {
        return tokenizedContent;
    }

    public void setTokenizedContent(String tokenizedContent) {
        this.tokenizedContent = tokenizedContent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isHasReport() {
        return hasReport;
    }

    public void setHasReport(boolean hasReport) {
        this.hasReport = hasReport;
    }

    public User getUploader() {
        return uploader;
    }

    public void setUploader(User uploader) {
        this.uploader = uploader;
    }
}