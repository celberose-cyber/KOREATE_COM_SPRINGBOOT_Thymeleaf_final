package org.zerock.com.example.user.my;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.zerock.com.example.common.DBUtil;
import org.zerock.com.example.user.GradePolicyDAO;
import org.zerock.com.example.user.GradePolicyDTO;
import org.zerock.com.example.user.UserDTO;

import java.sql.Connection;

@Controller
public class MyController {

    @GetMapping("/my")
    public String myPage(HttpSession session, Model model) throws Exception {
        UserDTO user = (UserDTO) session.getAttribute("LOGIN_USER");
        if (user == null) return "redirect:/login";

        // ✅ totalSpent 기준으로 정책 1개 조회
        GradePolicyDTO policy;
        try (Connection con = DBUtil.getConnection()) {
            policy = new GradePolicyDAO().findPolicyByTotalSpent(con, user.getTotalSpent());
        }

        // ✅ 화면에서 쓸 수 있게 넘김
        model.addAttribute("user", user);
        model.addAttribute("policy", policy);

        return "user/my";
    }
}
