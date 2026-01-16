package org.zerock.com.example.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("currentUrl")
    public String currentUrl(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String qs = request.getQueryString();
        return (qs == null || qs.isBlank()) ? uri : (uri + "?" + qs);
    }
}