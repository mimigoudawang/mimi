package com.compare.controller;

import com.compare.entity.Document;
import com.compare.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 数据库控制器
 * 处理数据库相关的请求，包括查看数据库内容和API访问
 */
@Controller
@RequestMapping("/database")
public class DatabaseController {

    @Autowired
    private DocumentService documentService;

    /**
     * 显示数据库内容页面
     * 
     * @param model Spring MVC模型对象
     * @return 视图名称
     */
    @GetMapping
    public String viewDatabase(Model model) {
        List<Document> documents = documentService.findAll();
        model.addAttribute("documents", documents);
        return "database";
    }

    /**
     * 提供文档数据的REST API端点
     * 
     * @return 文档列表
     */
    @GetMapping("/api/documents")
    @ResponseBody
    public List<Document> getDocuments() {
        return documentService.findAll();
    }
}