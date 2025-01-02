package com.compare.service;

import com.compare.entity.Document;
import com.compare.entity.CompareReport;
import com.compare.entity.User;
import com.compare.repository.DocumentRepository;
import com.compare.repository.CompareReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

/**
 * 文档服务类
 * 处理文档的上传、保存、查询等业务逻辑
 */
@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private CompareReportRepository compareReportRepository;

    /**
     * 获取所有文档
     * 
     * @return 系统中所有文档的列表
     */
    public List<Document> findAll() {
        return documentRepository.findAll();
    }

    /**
     * 保存文档实体
     * 
     * @param document 要保存的文档对象
     * @return 保存后的文档对象（包含生成的ID）
     */
    public Document save(Document document) {
        return documentRepository.save(document);
    }

    /**
     * 获取系统中的文档总数
     * 
     * @return 文档总数
     */
    public long count() {
        return documentRepository.count();
    }

    /**
     * 获取所有文档列表
     * 
     * @return 所有文档的列表
     */
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    /**
     * 保存上传的文档文件
     * 支持 .doc 和 .docx 格式的文件
     * 
     * @param file     上传的文件对象
     * @param uploader 上传文件的用户
     * @return 保存后的文档对象
     * @throws IOException 当文件读取或解析失败时抛出
     */
    public Document saveDocument(MultipartFile file, User uploader) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IOException("文件名不能为空");
        }

        Document doc = new Document();
        doc.setFileName(fileName);

        // 根据文件类型选择不同的解析方式
        String content;
        if (fileName.toLowerCase().endsWith(".doc")) {
            // 解析 .doc 文件
            try (POIFSFileSystem fs = new POIFSFileSystem(file.getInputStream());
                    HWPFDocument wordDoc = new HWPFDocument(fs)) {
                content = wordDoc.getDocumentText();
            }
        } else {
            // 解析 .docx 文件
            try (XWPFDocument wordDoc = new XWPFDocument(file.getInputStream())) {
                StringBuilder sb = new StringBuilder();
                for (XWPFParagraph para : wordDoc.getParagraphs()) {
                    sb.append(para.getText()).append("\n");
                }
                content = sb.toString();
            }
        }
        doc.setContent(content);
        doc.setUploadTime(LocalDateTime.now());
        doc.setUploader(uploader);
        System.out.println("文档内容长度: " + content.length());
        return documentRepository.save(doc);
    }

    /**
     * 根据ID查找文档
     * 
     * @param id 文档ID
     * @return 可能包含文档的 Optional 对象
     */
    public java.util.Optional<Document> findById(Long id) {
        return documentRepository.findById(id);
    }

    /**
     * 根据ID查找查重报告
     * 
     * @param id 报告ID
     * @return 可能包含报告的 Optional 对象
     */
    public java.util.Optional<CompareReport> findReportById(Long id) {
        return compareReportRepository.findById(id);
    }

    /**
     * 获取用户的文档列表
     * 如果是管理员用户，返回所有文档；否则只返回用户自己的文档
     * 
     * @param user 用户对象
     * @return 文档列表
     */
    public List<Document> getDocumentsByUser(User user) {
        if (user.getRole().equals("ROLE_ADMIN")) {
            return documentRepository.findAll();
        }
        return documentRepository.findByUploader(user);
    }

    /**
     * 根据ID删除文档
     * 
     * @param id 要删除的文档ID
     */
    public void deleteById(Long id) {
        documentRepository.deleteById(id);
    }

    /**
     * 批量删除文档
     * 
     * @param ids 要删除的文档ID列表
     */
    public void deleteAllById(List<Long> ids) {
        documentRepository.deleteAllById(ids);
    }

    /**
     * 检查指定ID的文档是否存在
     * 
     * @param id 文档ID
     * @return 如果文档存在返回true，否则返回false
     */
    public boolean existsById(Long id) {
        return documentRepository.existsById(id);
    }
}