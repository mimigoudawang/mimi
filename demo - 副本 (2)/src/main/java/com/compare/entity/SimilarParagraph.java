package com.compare.entity;

public class SimilarParagraph {
    private String originalText;
    private String similarText;
    private double similarity;
    private int originalIndex;
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