package com.compare.service;

import com.compare.entity.Document;
import com.compare.entity.CompareReport;
import com.compare.repository.CompareReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 文档比较服务
 * 实现文档相似度计算和报告生成功能
 */
@Service
public class CompareService {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private CompareReportRepository reportRepository;

    /**
     * 根据ID查找报告
     */
    public Optional<CompareReport> findReportById(Long id) {
        return reportRepository.findById(id);
    }

    /**
     * 生成文档比对报告
     * 计算文档相似度并生成详细报告
     */
    public CompareReport generateReport(Document userDoc) {
        try {
            CompareReport report = new CompareReport();
            report.setDocumentName(userDoc.getFileName());
            report.setCompareTime(LocalDateTime.now());

            StringBuilder reportContent = new StringBuilder();
            reportContent.append(
                    "<div class='report-container' style='max-width: 800px; margin: 0 auto; padding: 20px;'>\n");

            List<Document> sourceDocuments = documentService.findAll();
            double totalSimilarity = 0.0;
            int paragraphCount = 0;

            String[] userParagraphs = userDoc.getContent().split("\n");
            for (int i = 0; i < userParagraphs.length; i++) {
                String paragraph = userParagraphs[i].trim();
                if (paragraph.isEmpty())
                    continue;

                String[] sentences = paragraph.split("。|！|？|；");
                double maxSimilarity = 0.0;
                boolean hasSimilarity = false;
                Document bestMatchDoc = null;
                String bestMatchParagraph = "";

                // 查找相似内容
                for (String sentence : sentences) {
                    if (sentence.trim().isEmpty())
                        continue;

                    for (Document sourceDoc : sourceDocuments) {
                        if (sourceDoc.getId().equals(userDoc.getId()))
                            continue;

                        String[] sourceParagraphs = sourceDoc.getContent().split("\n");
                        for (String sourceParagraph : sourceParagraphs) {
                            if (sourceParagraph.trim().isEmpty())
                                continue;

                            double similarity = calculateSimilarity(
                                    tokenizeContent(sentence),
                                    tokenizeContent(sourceParagraph));
                            if (similarity > maxSimilarity) {
                                maxSimilarity = similarity;
                                bestMatchDoc = sourceDoc;
                                bestMatchParagraph = sourceParagraph;
                            }
                            if (isSimilar(similarity)) {
                                hasSimilarity = true;
                            }
                        }
                    }
                }

                // 开始生成报告内容
                reportContent.append(
                        "<div class='comparison-block' style='display: flex; margin-bottom: 20px; gap: 20px;'>\n" +
                                "  <div class='user-text' style='flex: 1; padding: 15px; border-radius: 8px; " +
                                "background-color: #f8f9fa; text-align: justify; line-height: 1.6;'>\n");

                // 添加用户文本，如果有相似内容则标色
                if (hasSimilarity && bestMatchDoc != null) {
                    String textColor = getTextColor(maxSimilarity);
                    reportContent.append("    <div style='color: " + textColor + "'>" + paragraph + "</div>\n");
                    totalSimilarity += maxSimilarity;
                    paragraphCount++;
                } else {
                    reportContent.append("    <div>" + paragraph + "</div>\n");
                }

                reportContent.append("  </div>\n");

                // 只在有相似内容时添加模板文本
                if (hasSimilarity && bestMatchDoc != null) {
                    reportContent.append(
                            "  <div class='template-text'>\n" +
                                    "    <div class='template-content'>" + bestMatchParagraph + "</div>\n" +
                                    "    <div class='source-info'>" +
                                    "      出处：" + bestMatchDoc.getFileName() +
                                    "    </div>\n" +
                                    "  </div>\n");
                } else {
                    // 没有相似内容时添加空的占位div保持对齐
                    reportContent.append(
                            "  <div class='template-text'></div>\n");
                }

                reportContent.append("</div>\n");
            }

            report.setMaxSimilarity(paragraphCount > 0 ? totalSimilarity / paragraphCount : 0);
            report.setReportContent(reportContent.toString());
            return reportRepository.save(report);
        } catch (Exception e) {
            throw new RuntimeException("生成报告失败: " + e.getMessage());
        }
    }

    /**
     * 计算两段文本的相似度
     * 使用混合算法：编辑距离 + 余弦相似度
     */
    private double calculateSimilarity(String text1, String text2) {
        // 预处理文本，移除所有空白字符和标点符号
        String processed1 = text1.replaceAll("[\\p{P}\\p{Z}\\s]+", "");
        String processed2 = text2.replaceAll("[\\p{P}\\p{Z}\\s]+", "");

        // 如果处理后的文本完全相同
        if (processed1.equals(processed2)) {
            return 1.0;
        }

        // 如果任一文本为空
        if (processed1.isEmpty() || processed2.isEmpty()) {
            return 0.0;
        }

        // 计算字符级别的编辑距离相似度
        double editSimilarity = calculateEditSimilarity(processed1, processed2);

        // 计算词级别的余弦相似度
        double cosineSimilarity = calculateCosineSimilarity(processed1, processed2);

        // 计算局部匹配度
        double localSimilarity = calculateLocalSimilarity(processed1, processed2);

        // 综合三种相似度，调整权重
        return 0.4 * editSimilarity + 0.3 * cosineSimilarity + 0.3 * localSimilarity;
    }

    /**
     * 计算编辑距离相似度
     */
    private double calculateEditSimilarity(String text1, String text2) {
        int[][] dp = new int[text1.length() + 1][text2.length() + 1];

        for (int i = 0; i <= text1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= text2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= text1.length(); i++) {
            for (int j = 1; j <= text2.length(); j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1] + 1,
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1));
                }
            }
        }

        int maxLength = Math.max(text1.length(), text2.length());
        return 1.0 - (double) dp[text1.length()][text2.length()] / maxLength;
    }

    /**
     * 计算余���相似度
     */
    private double calculateCosineSimilarity(String text1, String text2) {
        // 分词并统计词频
        Map<String, Integer> vector1 = getTermFrequencyMap(text1);
        Map<String, Integer> vector2 = getTermFrequencyMap(text2);

        // 计算点积
        double dotProduct = 0.0;
        for (Map.Entry<String, Integer> entry : vector1.entrySet()) {
            if (vector2.containsKey(entry.getKey())) {
                dotProduct += entry.getValue() * vector2.get(entry.getKey());
            }
        }

        // 计算向量模长
        double norm1 = 0.0, norm2 = 0.0;
        for (int value : vector1.values()) {
            norm1 += value * value;
        }
        for (int value : vector2.values()) {
            norm2 += value * value;
        }
        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);

        // 计算余弦相似度
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        return dotProduct / (norm1 * norm2);
    }

    /**
     * 获取文本的词频统计
     */
    private Map<String, Integer> getTermFrequencyMap(String text) {
        Map<String, Integer> frequencies = new HashMap<>();

        // 使用字符级别的n-gram分词（2-gram）
        for (int i = 0; i < text.length() - 1; i++) {
            String term = text.substring(i, i + 2);
            frequencies.merge(term, 1, Integer::sum);
        }

        // 使用滑动窗口获取更长的词组
        int windowSize = 3;
        List<String> chunks = getTextChunks(text, windowSize);
        for (String chunk : chunks) {
            frequencies.merge(chunk, 1, Integer::sum);
        }

        return frequencies;
    }

    /**
     * 获取文本的滑动窗口分块
     */
    private List<String> getTextChunks(String text, int windowSize) {
        List<String> chunks = new ArrayList<>();
        if (text.length() < windowSize) {
            chunks.add(text);
            return chunks;
        }

        for (int i = 0; i <= text.length() - windowSize; i++) {
            chunks.add(text.substring(i, i + windowSize));
        }
        return chunks;
    }

    /**
     * 调整相似度阈值
     */
    private boolean isSimilar(double similarity) {
        return similarity > 0.15; // 降低相似度阈值，提高灵敏度
    }

    /**
     * 根据相似度返回应的颜色代码
     */
    private String getTextColor(double similarity) {
        if (similarity >= 0.6)
            return "#ff4444"; // 红色
        if (similarity >= 0.3)
            return "#ff8800"; // 橘色
        return "#28a745"; // 绿色
    }

    /**
     * 对文本进行分词处理
     */
    private String tokenizeContent(String content) {
        return content.replaceAll("[\\p{P}\\p{S}]", " ")
                .replaceAll("\\s+", " ")
                .toLowerCase()
                .trim();
    }

    /**
     * 计算局部匹配度（处理同义改写的情况）
     */
    private double calculateLocalSimilarity(String text1, String text2) {
        int windowSize = 3; // 滑动窗口大小
        List<String> chunks1 = getTextChunks(text1, windowSize);
        List<String> chunks2 = getTextChunks(text2, windowSize);

        int matchCount = 0;
        for (String chunk : chunks1) {
            if (chunks2.contains(chunk)) {
                matchCount++;
            }
        }

        return chunks1.isEmpty() ? 0.0 : (double) matchCount / chunks1.size();
    }
}