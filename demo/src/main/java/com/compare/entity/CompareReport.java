package com.compare.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 比对报告实体类
 * 用于存储文档相似度比对的详细结果信息
 * 包含文档基本信息、比对时间、相似度等关键数据
 */
@Entity
@Table(name = "compare_reports")
public class CompareReport {

    /**
     * 报告唯一标识ID
     * 使用自增长策略生成
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 被比对文档的名称
     * 不允许为空
     */
    @Column(nullable = false)
    private String documentName;

    /**
     * 文档比对的执行时间
     * 记录比对操作的具体时间点
     */
    @Column(nullable = false)
    private LocalDateTime compareTime;

    /**
     * 文档比对的最高相似度
     * 取值范围: 0.0 - 1.0
     */
    @Column(nullable = false)
    private double maxSimilarity;

    /**
     * 比对报告的详细内容
     * 使用TEXT类型存储长文本
     * 包含详细的比对结果信息
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String reportContent;

    /**
     * 相似段落列表
     * 仅在内存中使用，不持久化到数据库
     * 用于存储文档中发现的相似段落信息
     */
    @Transient
    private List<SimilarParagraph> similarParagraphs = new ArrayList<>();

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public LocalDateTime getCompareTime() {
        return compareTime;
    }

    public void setCompareTime(LocalDateTime compareTime) {
        this.compareTime = compareTime;
    }

    public double getMaxSimilarity() {
        return maxSimilarity;
    }

    public void setMaxSimilarity(double maxSimilarity) {
        this.maxSimilarity = maxSimilarity;
    }

    public String getReportContent() {
        return reportContent;
    }

    public void setReportContent(String reportContent) {
        this.reportContent = reportContent;
    }

    public List<SimilarParagraph> getSimilarParagraphs() {
        return similarParagraphs;
    }

    public void setSimilarParagraphs(List<SimilarParagraph> similarParagraphs) {
        this.similarParagraphs = similarParagraphs;
    }
}