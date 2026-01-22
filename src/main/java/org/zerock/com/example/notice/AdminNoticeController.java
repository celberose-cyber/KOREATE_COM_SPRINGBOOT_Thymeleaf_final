package org.zerock.com.example.notice;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.com.example.common.NoticeDAO;
import org.zerock.com.example.user.UserDTO;

import java.util.LinkedHashMap;

@Controller
@RequestMapping("/admin/notices")
public class AdminNoticeController {

    private final NoticeDAO noticeDAO;

    public AdminNoticeController(NoticeDAO noticeDAO) {
        this.noticeDAO = noticeDAO;
    }

    private UserDTO login(HttpSession session) {
        Object obj = session.getAttribute("LOGIN_USER");
        return (obj instanceof UserDTO u) ? u : null;
    }
    private boolean isAdmin(HttpSession session) {
        UserDTO u = login(session);
        return u != null && "ADMIN".equalsIgnoreCase(u.getRole());
    }

    @GetMapping
    public String list(@RequestParam(defaultValue="1") int page,
                       @RequestParam(defaultValue="20") int size,
                       HttpSession session,
                       Model model) throws Exception {
        if (!isAdmin(session)) return "redirect:/login";

        int p = Math.max(1, page);
        int s = Math.min(100, Math.max(5, size));
        int offset = (p - 1) * s;

        int total = noticeDAO.countAll();
        int lastPage = (int)Math.ceil(total / (double)s);
        model.addAttribute("baseUrl", "/admin/notices");
        model.addAttribute("params", new LinkedHashMap<String, Object>()); // 현재는 빈 맵
        model.addAttribute("title", "공지사항 관리");
        model.addAttribute("list", noticeDAO.listPaged(s, offset));
        model.addAttribute("page", p);
        model.addAttribute("size", s);
        model.addAttribute("total", total);
        model.addAttribute("lastPage", Math.max(1, lastPage));
        return "admin/notices_list";
    }

    @GetMapping("/new")
    public String newForm(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/login";
        model.addAttribute("title", "공지 작성");
        return "admin/notice_form";
    }

    @PostMapping
    public String create(@RequestParam String title,
                         @RequestParam String content,
                         HttpSession session) throws Exception {
        if (!isAdmin(session)) return "redirect:/login";
        UserDTO me = login(session);

        noticeDAO.insert(title, content, me.getUserId());
        session.setAttribute("FLASH_MESSAGE", "공지 등록 완료");
        return "redirect:/admin/notices";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, HttpSession session, Model model) throws Exception {
        if (!isAdmin(session)) return "redirect:/login";
        var n = noticeDAO.findById(id);
        if (n == null) return "redirect:/admin/notices";

        model.addAttribute("title", "공지 수정");
        model.addAttribute("n", n);
        return "admin/notice_edit";
    }

    // ✅ 수정 처리 경로를 /{id}/edit 로 통일
    @PostMapping("/{id}/edit")
    public String update(@PathVariable long id,
                         @RequestParam String title,
                         @RequestParam String content,
                         HttpSession session) throws Exception {
        if (!isAdmin(session)) return "redirect:/login";

        noticeDAO.update(id, title, content);
        session.setAttribute("FLASH_MESSAGE", "공지 수정 완료");
        return "redirect:/admin/notices";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, HttpSession session) throws Exception {
        if (!isAdmin(session)) return "redirect:/login";

        noticeDAO.delete(id);
        session.setAttribute("FLASH_MESSAGE", "공지 삭제 완료");
        return "redirect:/admin/notices";
    }
}

