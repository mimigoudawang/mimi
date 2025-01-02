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
 * 处理文档相关的业务逻辑
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
     * @return 文档列表
     */
    public List<Document> findAll() {
        return documentRepository.findAll();
    }

    /**
     * 保存文档
     * 
     * @param document 要保存的文档
     * @return 保存后的文档
     */
    public Document save(Document document) {
        return documentRepository.save(document);
    }

    /**
     * 获取文档数量
     * 
     * @return 文档数量
     */
    public long count() {
        return documentRepository.count();
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public Document saveDocument(MultipartFile file, User uploader) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IOException("文件名不能为空");
        }

        Document doc = new Document();
        doc.setFileName(fileName);

        // 读取文件内容
        String content;
        if (fileName.toLowerCase().endsWith(".doc")) {
            try (POIFSFileSystem fs = new POIFSFileSystem(file.getInputStream());
                    HWPFDocument wordDoc = new HWPFDocument(fs)) {
                content = wordDoc.getDocumentText();
            }
        } else {
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

    public java.util.Optional<Document> findById(Long id) {
        return documentRepository.findById(id);
    }

    public java.util.Optional<CompareReport> findReportById(Long id) {
        return compareReportRepository.findById(id);
    }

    public List<Document> getDocumentsByUser(User user) {
        if (user.getRole().equals("ROLE_ADMIN")) {
            return documentRepository.findAll();
        }
        return documentRepository.findByUploader(user);
    }

    public void deleteById(Long id) {
        documentRepository.deleteById(id);
    }

    public void deleteAllById(List<Long> ids) {
        documentRepository.deleteAllById(ids);
    }

    public boolean existsById(Long id) {
        return documentRepository.existsById(id);
    }

}