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

    @GetMapping({ "/user", "/user/home" })
    public String userHome(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));
        List<UserDocument> documents = userDocumentRepository.findByUser(currentUser);
        model.addAttribute("documents", documents);
        return "user/home";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return "redirect:/user?error=empty";
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null
                    || (!fileName.toLowerCase().endsWith(".doc") && !fileName.toLowerCase().endsWith(".docx"))) {
                return "redirect:/user?error=type";
            }

            // 获取当前用户
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

            // 读取文件内容用于生成报告
            String content = readDocContent(file);

            // 创建临时文档对象用于生成报告
            Document tempDoc = new Document();
            tempDoc.setFileName(fileName);
            tempDoc.setContent(content);
            tempDoc.setUploadTime(LocalDateTime.now());

            // 保存用户文档记录
            UserDocument userDoc = new UserDocument();
            userDoc.setFileName(fileName);
            userDoc.setUploadTime(LocalDateTime.now());
            userDoc.setUser(currentUser);

            // 生成报告
            CompareReport report = compareService.generateReport(tempDoc);
            if (report != null && report.getId() != null) {
                userDoc.setHasReport(true);
                userDoc.setReportId(report.getId());
                userDocumentRepository.save(userDoc);
                return "redirect:/user/report/" + report.getId();
            }
            return "redirect:/user?error=report";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/user?error=upload";
        }
    }

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

    @GetMapping("/user/report/{id}")
    public String viewReport(@PathVariable Long id, Model model) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

            // 检查用户文档记录是否存在
            userDocumentRepository.findByUserAndReportId(currentUser, id)
                    .orElseThrow(() -> new RuntimeException("报告不存在或无权访问"));

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
                return "redirect:/user?error=report_deleted";
            }
            return "redirect:/user?error=report";
        }
    }

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

    @PostMapping("/user/reports/batch-delete")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> batchDeleteReports(@RequestBody List<Long> ids) {
        try {
            // 获取当前用户
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 验证所有报告是否属于当前用户
            for (Long id : ids) {
                userDocumentRepository.findByUserAndReportId(currentUser, id)
                        .orElseThrow(() -> new RuntimeException("报告不存在或无权访问"));
            }

            // 删除报告和用户文档记录
            for (Long id : ids) {
                userDocumentRepository.findByUserAndReportId(currentUser, id)
                        .ifPresent(doc -> {
                            reportRepository.deleteById(id);
                            userDocumentRepository.delete(doc);
                        });
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("删除失败: " + e.getMessage());
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
}