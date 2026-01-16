package org.zerock.com.example.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {

        HttpSession session = req.getSession(false);
        UserDTO user = (session == null) ? null : (UserDTO) session.getAttribute("LOGIN_USER");

        // 로그인 안 됨
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return false;
        }

        // ADMIN 체크
        String role = user.getRole();
        boolean isAdmin = (role != null && role.equalsIgnoreCase("ADMIN"));

        if (!isAdmin) {
            // 권한 없음 처리: 메인으로 보내기
            resp.sendRedirect(req.getContextPath() + "/");
            return false;
        }

        return true;
    }
}
