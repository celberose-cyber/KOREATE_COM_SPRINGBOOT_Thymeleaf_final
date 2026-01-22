package org.zerock.com.example.notice;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.com.example.common.NoticeDAO;

@Controller
@RequestMapping("/notices")
public class NoticeController {

    private final NoticeDAO noticeDAO;

    public NoticeController(NoticeDAO noticeDAO) {
        this.noticeDAO = noticeDAO;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue="1") int page,
                       @RequestParam(defaultValue="20") int size,
                       Model model) throws Exception {

        int p = Math.max(1, page);
        int s = Math.min(100, Math.max(5, size));
        int offset = (p - 1) * s;

        int total = noticeDAO.countAll();
        int lastPage = (int) Math.ceil(total / (double) s);

        model.addAttribute("title", "공지사항");
        model.addAttribute("list", noticeDAO.listPaged(s, offset));
        model.addAttribute("page", p);
        model.addAttribute("size", s);
        model.addAttribute("total", total);
        model.addAttribute("lastPage", Math.max(1, lastPage));

        return "notice/list";
    }


    @GetMapping("/{id}")
    public String detail(@PathVariable long id,
                         @RequestParam(required=false) Integer page,
                         @RequestParam(required=false) Integer size,
                         Model model) throws Exception {
        var n = noticeDAO.findById(id);
        if (n == null) return "redirect:/notices";

        model.addAttribute("title", "공지 상세");
        model.addAttribute("n", n);

        String returnUrl = "/notices";
        if (page != null || size != null) {
            int p = (page == null ? 1 : page);
            int s = (size == null ? 20 : size);
            returnUrl = "/notices?page=" + p + "&size=" + s;
        }
        model.addAttribute("returnUrl", returnUrl);

        return "notice/detail";
    }

}
