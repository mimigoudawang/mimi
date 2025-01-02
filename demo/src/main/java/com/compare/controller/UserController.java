package com.compare.controller;

import com.compare.entity.Document;
import com.compare.service.DocumentService;
import com.compare.service.CompareService;
import com.compare.entity.CompareReport;
import com.compare.entity.User;
import com.compare.repository.UserRepository;
import com.compare.repository.UserDocumentRepository;
import com.compare.repository.CompareReportRepository;
import com.compare.entity.UserDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;
import java.io.IOException;
import java.time.LocalDateTime;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import java.util.Map;
import java.util.HashMap;
import java.security.Principal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.CompletableFuture;
import java.io.ByteArrayInputStream;

/**
 * 用户控制器
 * 处理用户相关的请求，包括文档上传、比对、报告查看等功能
 */
@Controller
public class UserController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private CompareService compareService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDocumentRepository userDocumentRepository;

    @Autowired
    private CompareReportRepository reportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 显示用户主页
     * 展示用户上传的所有文档列表
     */
    @GetMapping({ "/user", "/user/home" })
    public String userHome(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));
        List<UserDocument> documents = userDocumentRepository.findByUserOrderByUploadTimeDesc(currentUser);
        model.addAttribute("documents", documents);
        return "user/home";
    }

    /**
     * 处理文件上传请求
     * 支持异步处理文档并生成比对报告
     * 
     * @param file 上传的文件
     * @return 包含处理结果的响应
     */
    @PostMapping("/upload")
    @ResponseBody
    public Map<String, String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("文件为空");
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null
                    || (!fileName.toLowerCase().endsWith(".doc") && !fileName.toLowerCase().endsWith(".docx"))) {
                throw new RuntimeException("不支持的文件类型");
            }

            // 先创建用户文档记录
            UserDocument userDoc = new UserDocument();
            userDoc.setFileName(fileName);
            userDoc.setUploadTime(LocalDateTime.now());
            userDoc.setUser(getCurrentUser());
            userDoc.setHasReport(false); // 初始状态设为false
            userDocumentRepository.save(userDoc);

            // 异步处理文件
            CompletableFuture.runAsync(() -> {
                try {
                    processFileAsync(file, userDoc);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            return response;
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return response;
        }
    }

    /**
     * 生成文档比对报告
     * 
     * @param id 文档ID
     */
    @GetMapping("/user/compare/{id}")
    public String compareDocument(@PathVariable Long id) {
        try {
            Document doc = documentService.findById(id)
                    .orElseThrow(() -> new RuntimeException("文档不存在"));
            CompareReport report = compareService.generateReport(doc);
            return "redirect:/user/report/" + report.getId();
        } catch (Exception e) {
            return "redirect:/user?error=compare";
        }
    }

    /**
     * 查看比对报告
     * 如果报告不存在，会删除相关的用户文档记录
     * 
     * @param id 报告ID
     */
    @GetMapping("/user/report/{id}")
    public String viewReport(@PathVariable Long id, Model model) {
        try {

            // 检查报告是否存在
            CompareReport report = compareService.findReportById(id)
                    .orElseThrow(() -> new RuntimeException("报告已被删除"));

            model.addAttribute("report", report);
            return "user/report";
        } catch (Exception e) {
            if (e.getMessage().equals("报告已被删除")) {
                // 如果报告被删除，删除用户文档记录
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User currentUser = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("用户不存在"));
                userDocumentRepository.findByUserAndReportId(currentUser, id)
                        .ifPresent(doc -> userDocumentRepository.delete(doc));
                return "redirect:/user";
            }
            return "redirect:/user";
        }
    }

    /**
     * 删除单个报告
     * 同时会删除相关的用户文档记录
     * 
     * @param id 报告ID
     */
    @PostMapping("/user/delete/{id}")
    public String deleteReport(@PathVariable Long id) {
        try {
            // 获取当前用户
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 验证报告是否属于当前用户
            UserDocument userDoc = userDocumentRepository.findByUserAndReportId(currentUser, id)
                    .orElseThrow(() -> new RuntimeException("报告不存在或无权访问"));

            // 删除报告和用户文档记录
            reportRepository.deleteById(id);
            userDocumentRepository.delete(userDoc);

            return "redirect:/user?success=delete";
        } catch (Exception e) {
            return "redirect:/user?error=delete";
        }
    }

    /**
     * 修改用户密码
     * 
     * @param request   包含旧密码和新密码的请求
     * @param principal 当前登录用户信息
     * @return 处理结果
     */
    @PostMapping("/user/change-password")
    @ResponseBody
    public Map<String, Object> changePassword(@RequestBody Map<String, String> request, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");

            User user = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 验证旧密码
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                response.put("success", false);
                response.put("message", "原密码错误");
                return response;
            }

            // 更新密码
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setOriginalPassword(newPassword);
            userRepository.save(user);

            response.put("success", true);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "密码修改失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 批量删除报告
     * 
     * @param ids 要删除的报告ID列表
     */
    @PostMapping("/user/reports/batch-delete")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> batchDeleteReports(@RequestBody List<Long> ids) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            for (Long id : ids) {
                // 先检查报告是否存在且属于当前用户
                UserDocument userDoc = userDocumentRepository.findByUserAndReportId(currentUser, id)
                        .orElseThrow(() -> new RuntimeException("报告不存在或无权访问"));

                // 删除报告和用户文档记录
                reportRepository.deleteById(id);
                userDocumentRepository.delete(userDoc);
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("删除失败: " + e.getMessage());
        }
    }

    /**
     * 刷新报告状态
     * 检查文档的报告是否已生成
     * 
     * @param docId 文档ID
     */
    @GetMapping("/user/refresh/{docId}")
    @ResponseBody
    public Map<String, Object> refreshReport(@PathVariable Long docId) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDocument doc = userDocumentRepository.findById(docId)
                    .orElseThrow(() -> new RuntimeException("文档不存在"));

            // 检查是否是当前用户的文档
            if (!doc.getUser().equals(getCurrentUser())) {
                throw new RuntimeException("无权访问此文档");
            }

            response.put("hasReport", doc.isHasReport());
            return response;
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return response;
        }
    }

    /**
     * 删除用户文档
     * 
     * @param id 文档ID
     */
    @PostMapping("/user/document/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        try {
            // 获取当前用户
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 查找并验证文档所有权
            UserDocument userDoc = userDocumentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("文档不存在"));

            if (!userDoc.getUser().equals(currentUser)) {
                throw new RuntimeException("无权访问此文档");
            }

            // 删除文档记录
            userDocumentRepository.delete(userDoc);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("删除失败: " + e.getMessage());
        }
    }

    /**
     * 异步处理上传的文件
     * 读取文件内容并生成比对报告
     * 
     * @param file    上传的文件
     * @param userDoc 用户文档记录
     */
    private void processFileAsync(MultipartFile file, UserDocument userDoc) throws Exception {
        try {
            String content = readDocContent(file);
            Document tempDoc = new Document();
            tempDoc.setFileName(file.getOriginalFilename());
            tempDoc.setContent(content);
            tempDoc.setUploadTime(LocalDateTime.now());

            CompareReport report = compareService.generateReport(tempDoc);

            if (report != null && report.getId() != null) {
                userDoc.setHasReport(true);
                userDoc.setReportId(report.getId());
                userDocumentRepository.save(userDoc);
            } else {
                // 如果报告生成失败，删除用户文档记录
                userDocumentRepository.delete(userDoc);
            }
        } catch (Exception e) {
            // 发生异常时，删除用户文档记录
            userDocumentRepository.delete(userDoc);
            throw e;
        }
    }

    /**
     * 读取文档内容
     * 支持doc和docx格式
     * 
     * @param file 上传的文件
     * @return 文档的文本内容
     */
    private String readDocContent(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IOException("文件名不能为空");
        }

        // 检查文件的实际内容类型
        byte[] bytes = file.getBytes();
        try {
            // 尝试检测文件类型
            if (isOLE2Document(bytes)) {
                return readDoc(bytes);
            } else if (isOOXMLDocument(bytes)) {
                return readDocx(file.getInputStream());
            } else {
                throw new IOException("不支持的文件格式");
            }
        } catch (Exception e) {
            throw new IOException("读取文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查是否是OLE2格式文档（doc）
     */
    private boolean isOLE2Document(byte[] bytes) {
        if (bytes.length < 8)
            return false;
        // OLE2 文件头标识
        return (bytes[0] == (byte) 0xD0 && bytes[1] == (byte) 0xCF &&
                bytes[2] == (byte) 0x11 && bytes[3] == (byte) 0xE0);
    }

    /**
     * 检查是否是OOXML格式文档（docx）
     */
    private boolean isOOXMLDocument(byte[] bytes) {
        if (bytes.length < 4)
            return false;
        // OOXML (docx) 文件头标识 (PK..)
        return (bytes[0] == 0x50 && bytes[1] == 0x4B &&
                bytes[2] == 0x03 && bytes[3] == 0x04);
    }

    /**
     * 读取doc格式文档内容
     */
    private String readDoc(byte[] bytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                POIFSFileSystem fs = new POIFSFileSystem(bais)) {
            HWPFDocument doc = new HWPFDocument(fs);
            org.apache.poi.hwpf.extractor.WordExtractor extractor = new org.apache.poi.hwpf.extractor.WordExtractor(
                    doc);

            StringBuilder content = new StringBuilder();
            for (String paragraph : extractor.getParagraphText()) {
                if (paragraph != null && !paragraph.trim().isEmpty()) {
                    content.append(paragraph.trim()).append("\n");
                }
            }
            extractor.close();
            doc.close();
            return content.toString();
        }
    }

    /**
     * 读取docx格式文档内容
     */
    private String readDocx(java.io.InputStream inputStream) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            StringBuilder content = new StringBuilder();
            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                content.append(paragraph.getText()).append("\n");
            }
            return content.toString();
        }
    }

    /**
     * 获取当前登录用户
     * 
     * @return 当前用户实体
     * @throws RuntimeException 如果用户不存在
     */
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));
    }
}