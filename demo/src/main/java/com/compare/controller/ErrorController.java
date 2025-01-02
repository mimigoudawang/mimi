package com.compare.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 错误处理控制器
 * 处理应用程序中的错误页面显示
 */
@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    /**
     * 处理错误请求
     * 
     * @return 错误页面视图名
     */
    @GetMapping("/error")
    public String handleError() {
        return "error";
    }
}