package org.zerock.com.example.suggest;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.com.example.common.SuggestCommentDAO;
import org.zerock.com.example.common.SuggestDAO;
import org.zerock.com.example.user.UserDTO;

import java.util.LinkedHashMap;

@Controller
@RequestMapping("/suggests")
public class SuggestController {

    private final SuggestDAO suggestDAO;
    private final SuggestCommentDAO commentDAO;

    public SuggestController(SuggestDAO suggestDAO, SuggestCommentDAO commentDAO) {
        this.suggestDAO = suggestDAO;
        this.commentDAO = commentDAO;
    }

    private UserDTO login(HttpSession session) {
        Object obj = session.getAttribute("LOGIN_USER");
        return (obj instanceof UserDTO u) ? u : null;
    }

    @GetMapping
    public String myList(@RequestParam(defaultValue="1") int page,
                         @RequestParam(defaultValue="20") int size,
                         HttpSession session,
                         Model model) throws Exception {

        UserDTO me = login(session);
        if (me == null) return "redirect:/login";

        int p = Math.max(1, page);
        int s = Math.min(100, Math.max(5, size));
        int offset = (p - 1) * s;

        int total = suggestDAO.countByUser(me.getUserId());
        int lastPage = (int) Math.ceil(total / (double) s);

        model.addAttribute("title", "건의사항");
        model.addAttribute("list", suggestDAO.listByUserPaged(me.getUserId(), s, offset));
        model.addAttribute("page", p);
        model.addAttribute("size", s);
        model.addAttribute("total", total);
        model.addAttribute("lastPage", Math.max(1, lastPage));

        return "suggest/list";
    }

    @GetMapping("/new")
    public String newForm(HttpSession session, Model model) {
        if (login(session) == null) return "redirect:/login";
        model.addAttribute("title", "건의 작성");
        model.addAttribute("params", new LinkedHashMap<String,Object>());

        return "suggest/form";
    }

    @PostMapping
    public String create(@RequestParam String title,
                         @RequestParam String content,
                         HttpSession session) throws Exception {
        UserDTO me = login(session);
        if (me == null) return "redirect:/login";

        suggestDAO.insert(me.getUserId(), title, content);
        session.setAttribute("FLASH_MESSAGE", "건의 등록 완료");
        return "redirect:/suggests";
    }


    @GetMapping("/{id}")
    public String detail(@PathVariable long id, HttpSession session, Model model) throws Exception {
        UserDTO me = login(session);
        if (me == null) return "redirect:/login";

        var s = suggestDAO.findById(id);
        if (s == null || s.getUserId() != me.getUserId()) {
            session.setAttribute("FLASH_MESSAGE", "권한이 없습니다.");
            return "redirect:/suggests";
        }

        model.addAttribute("title", "건의 상세");
        model.addAttribute("s", s);
        model.addAttribute("comments", commentDAO.listBySuggest(id));
        return "suggest/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, HttpSession session, Model model) throws Exception {
        UserDTO me = login(session);
        if (me == null) return "redirect:/login";

        var s = suggestDAO.findById(id);
        if (s == null || s.getUserId() != me.getUserId()) return "redirect:/suggests";

        model.addAttribute("title", "건의 수정");
        model.addAttribute("s", s);
        return "suggest/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable long id,
                         @RequestParam String title,
                         @RequestParam String content,
                         HttpSession session) throws Exception {
        UserDTO me = login(session);
        if (me == null) return "redirect:/login";

        var s = suggestDAO.findById(id);
        if (s == null || s.getUserId() != me.getUserId()) return "redirect:/suggests";

        suggestDAO.update(id, title, content);
        session.setAttribute("FLASH_MESSAGE", "수정 완료");
        return "redirect:/suggests/" + id;
    }


    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, HttpSession session) throws Exception {
        UserDTO me = login(session);
        if (me == null) return "redirect:/login";

        var s = suggestDAO.findById(id);
        if (s == null || s.getUserId() != me.getUserId()) return "redirect:/suggests";

        suggestDAO.delete(id);
        session.setAttribute("FLASH_MESSAGE", "삭제 완료");
        return "redirect:/suggests";
    }

    // 댓글 작성(유저): 내 글에만 가능
    @PostMapping("/{id}/comments")
    public String addComment(@PathVariable long id,
                             @RequestParam String content,
                             HttpSession session) throws Exception {

        UserDTO me = login(session);
        if (me == null) return "redirect:/login";

        var s = suggestDAO.findById(id);
        if (s == null || s.getUserId() != me.getUserId()) return "redirect:/suggests";

        String c = (content == null) ? "" : content.trim();
        if (!c.isEmpty()) {
            commentDAO.insert(id, me.getUserId(), "USER", c);
        }
        return "redirect:/suggests/" + id;
    }
}
