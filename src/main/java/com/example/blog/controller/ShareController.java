package com.example.blog.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/share")
public class ShareController {

    @GetMapping
    public void shareRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String userAgent = request.getHeader("User-Agent").toLowerCase();

        boolean isIOS = userAgent.contains("iphone") || userAgent.contains("ipad");

        if (isIOS) {
            // Try to open the iOS app
            response.sendRedirect("trasora://create");
        } else {
            // Web fallback
            response.sendRedirect("https://www.trasora.com/create");
        }
    }
}
