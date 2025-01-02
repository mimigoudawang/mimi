package com.compare.controller;

import com.compare.entity.Document;
import com.compare.entity.CompareReport;
import com.compare.entity.User;
import com.compare.service.DocumentService;
import com.compare.repository.CompareReportRepository;
import com.compare.repository.UserRepository;
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
 * 处理所有管理员相关的请求，包括用户管理、报告管理、文档管理等功能
 * 
 * @author system
 * @version 1.0
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompareReportRepository reportRepository;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 显示管理后台主页
     * 加载所有用户、比对报告和模板文档数据
     * 
     * @param model Spring MVC模型对象，用于向视图传递数据
     * @return 返回管理后台首页视图名称
     */
    @GetMapping({ "", "/", "/index" })
    public String adminHome(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("reports", reportRepository.findAll());
        model.addAttribute("templates", documentService.findAll());
        return "admin/index";
    }

    /**
     * 删除指定用户
     * 注意：不允许删除管理员账户
     * 
     * @param id 要删除的用户ID
     * @return ResponseEntity 包含删除操作的结果
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

            // 删除用户
            userRepository.delete(user);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("删除失败: " + e.getMessage());
        }
    }

    /**
     * 删除指定的比对报告
     * 
     * @param id 要删除的报告ID
     * @return ResponseEntity 包含删除操作的结果
     */
    @PostMapping("/reports/delete/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> deleteReport(@PathVariable Long id) {
        try {
            if (!reportRepository.existsById(id)) {
                return ResponseEntity.badRequest().body("报告不存在");
            }

            reportRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("删除失败: " + e.getMessage());
        }
    }

    /**
     * 删除指定的模板文档
     * 
     * @param id 要删除的模板ID
     * @return ResponseEntity 包含删除操作的结果
     */
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

    /**
     * 查看指定报告的详细信息
     * 
     * @param id    报告ID
     * @param model Spring MVC模型对象
     * @return 报告详情页面视图名称
     */
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

    /**
     * 上传模板文档
     * 支持.doc和.docx格式的文件
     * 
     * @param files 上传的文件数组
     * @return ResponseEntity 包含上传操作的结果
     */
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

    /**
     * 读取Word文档内容
     * 支持.doc和.docx格式
     * 
     * @param file 上传的文件
     * @return 文档的文本内容
     * @throws IOException 文件读取异常
     */
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

    /**
     * 批量删除用户
     * 已废弃，请使用 batchDeleteUsers 方法
     * 
     * @param ids 要删除的用户ID列表
     * @return ResponseEntity 包含删除操作的结果
     * @deprecated 使用 {@link #batchDeleteUsers(List)} 替代
     */
    @Deprecated
    @PostMapping("/batch-delete")
    @ResponseBody
    public ResponseEntity<?> batchDelete(@RequestBody List<Long> ids) {
        try {
            for (Long id : ids) {

                userRepository.deleteById(id);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 批量删除用户
     * 注意：不会删除管理员账户
     * 
     * @param ids 要删除的用户ID列表
     * @return ResponseEntity 包含删除操作的结果
     */
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

                userRepository.deleteById(id);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 批量删除比对报告
     * 
     * @param ids 要删除的报告ID列表
     * @return ResponseEntity 包含删除操作的结果
     */
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

            // 删除报告
            reportRepository.deleteAllById(ids);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除模板文档
     * 
     * @param ids 要删除的模板ID列表
     * @return ResponseEntity 包含删除操作的结果
     */
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
     * 
     * @param id      用户ID
     * @param request 包含新密码的请求体
     * @return ResponseEntity 包含密码重置操作的结果
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
     * 获取模板文档内容
     * 
     * @param id 模板ID
     * @return ResponseEntity 包含模板内容的响应
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
