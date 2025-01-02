package com.compare.entity;

/**
 * 相似段落实体类
 * 用于存储原文和相似文本的段落比较结果
 */
public class SimilarParagraph {
    /**
     * 原文段落文本
     */
    private String originalText;

    /**
     * 相似的段落文本
     */
    private String similarText;

    /**
     * 两段文本的相似度
     * 取值范围: 0.0 - 1.0
     */
    private double similarity;

    /**
     * 原文段落在文档中的索引位置
     */
    private int originalIndex;

    /**
     * 相似段落在比较文档中的索引位置
     */
    private int similarIndex;

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getSimilarText() {
        return similarText;
    }

    public void setSimilarText(String similarText) {
        this.similarText = similarText;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    public int getOriginalIndex() {
        return originalIndex;
    }

    public void setOriginalIndex(int originalIndex) {
        this.originalIndex = originalIndex;
    }

    public int getSimilarIndex() {
        return similarIndex;
    }

    public void setSimilarIndex(int similarIndex) {
        this.similarIndex = similarIndex;
    }
}