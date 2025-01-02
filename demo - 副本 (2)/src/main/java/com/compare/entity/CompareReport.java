package com.compare.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 比对报告实体类
 * 存储文档比对的结果
 */
@Entity
@Table(name = "compare_reports")
public class CompareReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String documentName;

    @Column(nullable = false)
    private LocalDateTime compareTime;

    @Column(nullable = false)
    private double maxSimilarity;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reportContent;

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