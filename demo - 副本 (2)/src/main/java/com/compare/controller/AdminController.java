package com.compare.controller;

import com.compare.entity.Document;
import com.compare.entity.CompareReport;
import com.compare.entity.User;
import com.compare.entity.UserDocument;
import com.compare.service.DocumentService;
import com.compare.repository.CompareReportRepository;
import com.compare.repository.UserRepository;
import com.compare.repository.UserDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.io.IOException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import java.util.Map;
import java.util.HashMap;

/**
 * 管理员控制器
 * 处理所有管理员相关的请求，包括用户管理、报告管理、文档管理等
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompareReportRepository reportRepository;

    @Autowired
    private UserDocumentRepository userDocumentRepository;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 显示管理后台主页
     * 
     * @param model Spring MVC模型
     * @return 管理后台页面
     */
    @GetMapping({ "", "/", "/index" })
    public String adminHome(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("reports", reportRepository.findAll());
        model.addAttribute("documents", userDocumentRepository.findAll());
        model.addAttribute("templates", documentService.findAll());
        return "admin/index";
    }

    /**
     * 删除用户
     * 同时删除用户相关的所有文档和报告
     */
    @PostMapping("/users/delete/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 不允许删除管理员
            if ("ROLE_ADMIN".equals(user.getRole())) {
                return ResponseEntity.badRequest().body("不能删除管理员账户");
            }

            // 删除用户相关的所有文档和报告
            List<UserDocument> userDocs = userDocumentRepository.findByUser(user);
            for (UserDocument doc : userDocs) {
                if (doc.isHasReport()) {
                    reportRepository.deleteById(doc.getReportId());
                }
            }
            userDocumentRepository.deleteAll(userDocs);
            userRepository.delete(user);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("删除失败");
        }
    }

    @PostMapping("/reports/delete/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> deleteReport(@PathVariable Long id) {
        try {
            if (!reportRepository.existsById(id)) {
                return ResponseEntity.badRequest().body("报告不存在");
            }

            // 删除相关的用户文档记录
            userDocumentRepository.findByReportId(id)
                    .ifPresent(doc -> userDocumentRepository.delete(doc));

            reportRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("删除失败: " + e.getMessage());
        }
    }

    @PostMapping("/documents/delete/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        try {
            if (!userDocumentRepository.existsById(id)) {
                return ResponseEntity.badRequest().body("文档不存在");
            }
            userDocumentRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("删除失败: " + e.getMessage());
        }
    }

    @PostMapping("/templates/delete/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id) {
        try {
            if (!documentService.existsById(id)) {
                return ResponseEntity.badRequest().body("模板不存在");
            }
            documentService.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/report/{id}")
    public String viewReport(@PathVariable Long id, Model model) {
        try {
            CompareReport report = reportRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("报告不存在"));
            model.addAttribute("report", report);
            return "admin/report";
        } catch (Exception e) {
            return "redirect:/admin?error=report";
        }
    }

    @PostMapping("/template/upload")
    public ResponseEntity<?> uploadTemplate(@RequestParam("files") MultipartFile[] files) {
        try {
            if (files.length == 0) {
                return ResponseEntity.badRequest().body("No files selected");
            }

            for (MultipartFile file : files) {
                String fileName = file.getOriginalFilename();
                if (fileName == null
                        || (!fileName.toLowerCase().endsWith(".doc") && !fileName.toLowerCase().endsWith(".docx"))) {
                    return ResponseEntity.badRequest().body("Invalid file type");
                }

                String content = readDocContent(file);
                Document doc = new Document();
                doc.setFileName(fileName);
                doc.setContent(content);
                doc.setUploadTime(LocalDateTime.now());
                documentService.save(doc);
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Upload failed");
        }
    }

    private String readDocContent(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IOException("文件名不能为空");
        }

        if (fileName.toLowerCase().endsWith(".docx")) {
            try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
                StringBuilder content = new StringBuilder();
                for (XWPFParagraph paragraph : doc.getParagraphs()) {
                    content.append(paragraph.getText()).append("\n");
                }
                return content.toString();
            }
        } else {
            try (POIFSFileSystem fs = new POIFSFileSystem(file.getInputStream());
                    HWPFDocument doc = new HWPFDocument(fs)) {
                return doc.getDocumentText();
            }
        }
    }

    @PostMapping("/batch-delete")
    @ResponseBody
    public ResponseEntity<?> batchDelete(@RequestBody List<Long> ids) {
        try {
            for (Long id : ids) {
                User user = userRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("用户不存在"));

                List<UserDocument> userDocs = userDocumentRepository.findByUser(user);
                for (UserDocument doc : userDocs) {
                    if (doc.isHasReport()) {
                        reportRepository.deleteById(doc.getReportId());
                    }
                }
                userDocumentRepository.deleteAll(userDocs);
                userRepository.deleteById(id);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/users/batch-delete")
    @ResponseBody
    public ResponseEntity<?> batchDeleteUsers(@RequestBody List<Long> ids) {
        try {
            for (Long id : ids) {
                User user = userRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("用户不存在"));

                // 不允许删除管理员
                if ("ROLE_ADMIN".equals(user.getRole())) {
                    continue;
                }

                List<UserDocument> userDocs = userDocumentRepository.findByUser(user);
                for (UserDocument doc : userDocs) {
                    if (doc.isHasReport()) {
                        reportRepository.deleteById(doc.getReportId());
                    }
                }
                userDocumentRepository.deleteAll(userDocs);
                userRepository.deleteById(id);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/reports/batch-delete")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> batchDeleteReports(@RequestBody List<Long> ids) {
        try {
            // 先验证所有ID是否存在
            for (Long id : ids) {
                if (!reportRepository.existsById(id)) {
                    return ResponseEntity.badRequest().body("报告ID不存在: " + id);
                }
            }

            // 删除相关的用户文档记录
            for (Long id : ids) {
                userDocumentRepository.findByReportId(id)
                        .ifPresent(doc -> userDocumentRepository.delete(doc));
            }

            // 删除报告
            reportRepository.deleteAllById(ids);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("删除失败: " + e.getMessage());
        }
    }

    @PostMapping("/documents/batch-delete")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> batchDeleteDocuments(@RequestBody List<Long> ids) {
        try {
            // 先验证所有ID是否存在
            for (Long id : ids) {
                if (!userDocumentRepository.existsById(id)) {
                    return ResponseEntity.badRequest().body("文档ID不存在: " + id);
                }
            }

            // 删除文档
            userDocumentRepository.deleteAllById(ids);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("删除失败: " + e.getMessage());
        }
    }

    @PostMapping("/templates/batch-delete")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> batchDeleteTemplates(@RequestBody List<Long> ids) {
        try {
            // 先验证所有ID是否存在
            for (Long id : ids) {
                if (!documentService.existsById(id)) {
                    return ResponseEntity.badRequest().body("模板ID不存在: " + id);
                }
            }

            // 删除模板
            documentService.deleteAllById(ids);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("删除失败: " + e.getMessage());
        }
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/users/{id}/reset-password")
    @ResponseBody
    public ResponseEntity<?> resetUserPassword(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            String newPassword = request.get("newPassword");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                throw new RuntimeException("新密码不能为空");
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            user.setOriginalPassword(newPassword);
            userRepository.save(user);

            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            response.put("message", "密码重置成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("success", "false");
            errorResponse.put("message", "密码重置失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 获取模板内容
     */
    @GetMapping("/templates/{id}/content")
    @ResponseBody
    public ResponseEntity<?> getTemplateContent(@PathVariable Long id) {
        try {
            Document template = documentService.findById(id)
                    .orElseThrow(() -> new RuntimeException("模板不存在"));

            Map<String, String> response = new HashMap<>();
            response.put("content", template.getContent());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取模板内容失败");
        }
    }
}
