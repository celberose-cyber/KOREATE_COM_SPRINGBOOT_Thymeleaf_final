package org.zerock.com.example.legal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/policy")
public class PolicyController {

    private final LegalService legalService;

    public PolicyController(LegalService legalService) {
        this.legalService = legalService;
    }

    @GetMapping("/terms")
    public String terms(Model model) throws Exception {
        var v = legalService.getActiveDocView("TERMS");
        if (v == null) {
            model.addAttribute("title", "이용약관");
            model.addAttribute("version", "");
            model.addAttribute("html", "<p>활성 이용약관이 없습니다. 관리자에게 문의하세요.</p>");
        } else {
            model.addAttribute("title", v.title);
            model.addAttribute("version", v.version);
            model.addAttribute("html", v.html);
        }
        return "legal/policy";
    }

    @GetMapping("/privacy")
    public String privacy(Model model) throws Exception {
        var v = legalService.getActiveDocView("PRIVACY");
        if (v == null) {
            model.addAttribute("title", "개인정보처리방침");
            model.addAttribute("version", "");
            model.addAttribute("html", "<p>활성 개인정보처리방침이 없습니다. 관리자에게 문의하세요.</p>");
        } else {
            model.addAttribute("title", v.title);
            model.addAttribute("version", v.version);
            model.addAttribute("html", v.html);
        }
        return "legal/policy";
    }
}
