package org.zerock.com.example.suggest;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.com.example.common.SuggestCommentDAO;
import org.zerock.com.example.common.SuggestDAO;
import org.zerock.com.example.user.UserDTO;
@Controller
@RequestMapping("/admin/suggests")
public class AdminSuggestController {

    private final SuggestDAO suggestDAO;
    private final SuggestCommentDAO commentDAO;

    public AdminSuggestController(SuggestDAO suggestDAO, SuggestCommentDAO commentDAO) {
        this.suggestDAO = suggestDAO;
        this.commentDAO = commentDAO;
    }

    private UserDTO login(HttpSession session) {
        Object obj = session.getAttribute("LOGIN_USER");
        return (obj instanceof UserDTO u) ? u : null;
    }

    private boolean isAdmin(HttpSession session) {
        UserDTO u = login(session);
        return u != null && "ADMIN".equalsIgnoreCase(u.getRole());
    }

    // ✅ 관리자 목록 (페이징 + 댓글 상태 포함)
    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size,
                       HttpSession session,
                       Model model) throws Exception {

        if (!isAdmin(session)) return "redirect:/login";

        int p = Math.max(1, page);
        int s = Math.min(100, Math.max(5, size));
        int offset = (p - 1) * s;

        int total = suggestDAO.countAll();
        int lastPage = (int) Math.ceil(total / (double) s);

        model.addAttribute("title", "건의사항 관리");
        model.addAttribute("list", suggestDAO.listPaged(s, offset)); // ✅ 핵심
        model.addAttribute("page", p);
        model.addAttribute("size", s);
        model.addAttribute("total", total);
        model.addAttribute("lastPage", Math.max(1, lastPage));
        model.addAttribute("baseUrl", "/admin/suggests");
        model.addAttribute("params", new java.util.LinkedHashMap<>());

        return "admin/suggests_list";
    }

    // ✅ 관리자 상세
    @GetMapping("/{id}")
    public String detail(@PathVariable long id,
                         HttpSession session,
                         Model model) throws Exception {

        if (!isAdmin(session)) return "redirect:/login";

        var s = suggestDAO.findById(id);
        if (s == null) return "redirect:/admin/suggests";

        model.addAttribute("title", "건의 상세");
        model.addAttribute("s", s);
        model.addAttribute("comments", commentDAO.listBySuggest(id));
        return "admin/suggest_detail";
    }
}




