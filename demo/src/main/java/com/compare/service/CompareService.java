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
            double weightedSimilaritySum = 0.0; // 加权相似度总和
            int totalLength = 0; // 总字符长度
            String[] userParagraphs = userDoc.getContent().split("\n");

            for (int i = 0; i < userParagraphs.length; i++) {
                String paragraph = userParagraphs[i].trim();
                if (paragraph.isEmpty())
                    continue;

                int paragraphLength = paragraph.length(); // 段落长度作为权重
                totalLength += paragraphLength;

                double paragraphMaxSimilarity = 0.0;
                Document bestMatchDoc = null;
                String bestMatchParagraph = "";

                // 查找相似内容
                for (Document sourceDoc : sourceDocuments) {
                    if (sourceDoc.getId().equals(userDoc.getId()))
                        continue;

                    String[] sourceParagraphs = sourceDoc.getContent().split("\n");
                    for (String sourceParagraph : sourceParagraphs) {
                        if (sourceParagraph.trim().isEmpty())
                            continue;

                        double similarity = calculateSimilarity(paragraph, sourceParagraph);
                        if (similarity > paragraphMaxSimilarity) {
                            paragraphMaxSimilarity = similarity;
                            bestMatchDoc = sourceDoc;
                            bestMatchParagraph = sourceParagraph;
                        }
                    }
                }

                // 计算加权相似度
                weightedSimilaritySum += paragraphMaxSimilarity * paragraphLength;

                // 显示所有段落，相似的显示对应模板
                reportContent.append(
                        "<div class='comparison-block' style='display: flex; margin-bottom: 20px; gap: 20px;'>\n");

                // 用户文本
                reportContent.append(
                        "<div class='user-text' style='flex: 1; padding: 15px; border-radius: 8px; " +
                                "background-color: #f8f9fa; text-align: justify; line-height: 1.6;'>\n");

                String textColor = isSimilar(paragraphMaxSimilarity) ? getTextColor(paragraphMaxSimilarity) : "#000000";
                reportContent.append("<div style='color: " + textColor + "'>" + paragraph + "</div>\n");
                reportContent.append("</div>\n");

                // 如果有相似内容，显示模板文本
                if (isSimilar(paragraphMaxSimilarity) && bestMatchDoc != null) {

                    reportContent.append(
                            "<div class='template-text' style='flex: 1;'>\n" +
                                    "<div class='template-content'>" + bestMatchParagraph + "</div>\n" +
                                    "<div class='source-info' style='text-align: right; color: #666;'>" +
                                    "出处：" + bestMatchDoc.getFileName() +
                                    " (相似度: " + String.format("%.1f", paragraphMaxSimilarity * 100) + "%)" +
                                    "</div>\n" +
                                    "</div>\n");
                } else {
                    reportContent.append("<div class='template-text' style='flex: 1;'></div>\n");
                }
                reportContent.append("</div>\n");
            }

            // 计算加权平均相似度
            double overallSimilarity = totalLength > 0 ? weightedSimilaritySum / totalLength : 0.0;
            report.setMaxSimilarity(overallSimilarity);

            report.setReportContent(reportContent.toString());
            return reportRepository.save(report);
        } catch (Exception e) {
            throw new RuntimeException("生成报告失败: " + e.getMessage());
        }
    }

    /**
     * 计算两段文本的相似度
     * 使用基于句子级别的相似度计算方法
     * 
     * @param text1 第一段文本
     * @param text2 第二段文本
     * @return 返回两段文本的相似度（0-1之间的浮点数）
     */
    private double calculateSimilarity(String text1, String text2) {
        // 预处理文本，过滤常见学术用语和格式
        text1 = preprocessAcademicText(text1);
        text2 = preprocessAcademicText(text2);

        // 分句并计算句子级别的相似度
        String[] sentences1 = text1.split("[。！？]");
        String[] sentences2 = text2.split("[。！？]");

        double totalSimilarity = 0.0;
        int validSentences = 0;

        for (String s1 : sentences1) {
            if (s1.trim().length() < 10)
                continue; // 忽略过短的句子

            double maxSentenceSimilarity = 0.0;
            for (String s2 : sentences2) {
                if (s2.trim().length() < 10)
                    continue;

                // 计算句子级别的相似度
                double sim = calculateSentenceSimilarity(s1, s2);
                maxSentenceSimilarity = Math.max(maxSentenceSimilarity, sim);
            }

            if (maxSentenceSimilarity > 0.3) { // 只统计相似度超过阈值的句子
                totalSimilarity += maxSentenceSimilarity;
                validSentences++;
            }
        }

        return validSentences > 0 ? totalSimilarity / validSentences : 0.0;
    }

    /**
     * 计算两个句子之间的相似度
     * 使用字符级别的n-gram和Jaccard相似度计算
     * 
     * @param s1 第一个句子
     * @param s2 第二个句子
     * @return 返回两个句子的相似度（0-1之间的浮点数）
     */
    private double calculateSentenceSimilarity(String s1, String s2) {
        // 使用字符级别的n-gram
        Set<String> ngrams1 = extractNgrams(s1);
        Set<String> ngrams2 = extractNgrams(s2);

        // 计算Jaccard相似度
        Set<String> union = new HashSet<>(ngrams1);
        union.addAll(ngrams2);

        Set<String> intersection = new HashSet<>(ngrams1);
        intersection.retainAll(ngrams2);

        return (double) intersection.size() / union.size();
    }

    /**
     * 从文本中提取n-gram特征
     * 使用2-gram和3-gram的组合方式
     * 
     * @param text 输入文本
     * @return 返回n-gram特征集合
     */
    private Set<String> extractNgrams(String text) {
        Set<String> ngrams = new HashSet<>();
        // 使用2-gram和3-gram的组合
        for (int i = 0; i < text.length() - 1; i++) {
            ngrams.add(text.substring(i, i + 2));
            if (i < text.length() - 2) {
                ngrams.add(text.substring(i, i + 3));
            }
        }
        return ngrams;
    }

    /**
     * 预处理文本，移除日期格式和常用语句
     * 1. 移除标点符号和空格
     * 2. 过滤常见学术用语和格式词
     * 3. 过滤参考文献格式
     * 4. 过滤章节标记和数字
     * 
     * @param text 输入文本
     * @return 返回预处理后的文本
     */
    private String preprocessAcademicText(String text) {
        // 移除标点和空格
        text = text.replaceAll("[\\p{P}\\p{Z}]", "");
        // 转为小写
        text = text.toLowerCase();
        // 移除空白字符
        text = text.replaceAll("\\s+", "").trim();

        // 过滤常见学术用语和格式词
        text = text.replaceAll("(?i)(摘要|关键词|引言|结论|参考文献|图|表|fig\\.|tab\\.)", "");
        text = text.replaceAll("(?i)(本文|研究|分析|实验|数据|方法|结果|讨论)", "");
        text = text.replaceAll("(?i)(第[一二三四五六七八九十]章|\\d+\\.\\d+)", "");

        // 过滤参考文献格式
        text = text.replaceAll("\\[\\d+\\]", "");
        text = text.replaceAll("\\d{4}年", "");

        // 过滤常见标点和格式符号
        text = text.replaceAll("[\\[\\]（）\\(\\)\\{\\}\"\"'']", "");

        return text;
    }

    /**
     * 调整相似度阈值
     * 判断两段文本是否相似
     * 
     * @param similarity 相似度值
     * @return 如果相似度大于阈值返回true，否则返回false
     */
    private boolean isSimilar(double similarity) {
        return similarity > 0.50; // 提高相似度阈值
    }

    /**
     * 根据相似度返回对应的颜色代码
     * 相似度>=0.70显示红色
     * 相似度>=0.50显示橘色
     * 其他显示绿色
     * 
     * @param similarity 相似度值
     * @return 返回对应的颜色代码
     */
    private String getTextColor(double similarity) {
        if (similarity >= 0.70)
            return "#ff4444"; // 红色
        if (similarity >= 0.50)
            return "#ff8800"; // 橘色
        return "#28a745"; // 绿色
    }

}