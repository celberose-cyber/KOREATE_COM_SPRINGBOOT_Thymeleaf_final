package org.zerock.com.example.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.com.example.legal.LegalService;

@Controller
public class AuthController {

    private static final String DOMAIN = "@koreate.com";

    private final UserDAO userDAO;
    private final RegisterService registerService;
    private final LegalService legalService;
    private final VerifyEmailService verifyEmailService;


    public AuthController(UserDAO userDAO,
                          RegisterService registerService,
                          LegalService legalService,
                          VerifyEmailService verifyEmailService) {
        this.userDAO = userDAO;
        this.registerService = registerService;
        this.legalService = legalService;
        this.verifyEmailService = verifyEmailService;
    }

    private String toKoreateEmail(String loginId) {
        if (loginId == null) return null;
        String v = loginId.trim().toLowerCase();

        // ✅ 이 칸은 "아이디(앞부분)" 용도라서 @가 들어오면 막는다
        if (v.contains("@")) {
            throw new IllegalArgumentException("아이디에는 @를 입력하지 마세요. 예: jack2");
        }
        return v + DOMAIN; // @koreate.com 붙임
    }



    @GetMapping("/login")
    public String loginForm() {
        return "user/login";
    }

    @PostMapping("/login")
    public String loginSubmit(
            @RequestParam String loginId,   // ✅ email -> loginId 로 변경 권장
            @RequestParam String password,
            HttpSession session,
            Model model
    ) throws Exception {

        String email = toKoreateEmail(loginId);

        String hash = PasswordUtil.sha256(password);
        UserDTO user = userDAO.findByEmailAndHash(email, hash);

        if (user == null) {
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
            return "user/login";
        }

        if (!user.isEmailVerified()) {
            model.addAttribute("error", "이메일 인증이 필요합니다. 메일함을 확인해주세요.");
            return "user/login";
        }

        session.setAttribute("LOGIN_USER", user);
        return "redirect:/";
    }

    @GetMapping("/register")
    public String registerForm(Model model) throws Exception {
        // ✅ 약관/개인정보 문서(활성 버전) 로드해서 스크롤 박스에 표시
        model.addAttribute("terms", legalService.getActiveDocView("TERMS"));
        model.addAttribute("privacy", legalService.getActiveDocView("PRIVACY"));
        return "user/register";
    }

    @PostMapping("/register")
    public String registerSubmit(
            @RequestParam String loginId,
            @RequestParam String password,
            @RequestParam String name,
            @RequestParam String phone,
            @RequestParam(required = false) String termsAgreed,
            @RequestParam(required = false) String privacyAgreed,
            @RequestParam(required = false) Long termsDocId,
            @RequestParam(required = false) Long privacyDocId,
            HttpServletRequest req,
            Model model
    ) throws Exception {

        // ✅ 1차 방어(컨트롤러에서 UX용 메시지)
        if (loginId.isBlank() || password.isBlank() || name.isBlank() || phone.isBlank()) {
            model.addAttribute("error", "모든 항목을 입력해주세요.");
            return registerWithDocs(model);
        }
        if (termsAgreed == null || privacyAgreed == null) {
            model.addAttribute("error", "이용약관/개인정보처리방침에 동의해야 가입할 수 있습니다.");
            return registerWithDocs(model);
        }

        String email = toKoreateEmail(loginId);

        RegisterService.RegisterCommand cmd = new RegisterService.RegisterCommand();
        cmd.email = email;
        cmd.passwordHash = PasswordUtil.sha256(password);
        cmd.name = name;
        cmd.phone = phone;
        cmd.ipAddr = req.getRemoteAddr();
        cmd.userAgent = req.getHeader("User-Agent");
        cmd.appBaseUrl = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort();

// ✅ 이 2줄이 없으면 서비스에서는 null로 들어감
        cmd.termsDocId = termsDocId;
        cmd.privacyDocId = privacyDocId;

        var result = registerService.register(cmd);

        if (!result.success) {
            model.addAttribute("error", result.message);
            return registerWithDocs(model);
        }

        model.addAttribute("message", result.message);
        return "user/login"; // 가입 후 안내 메시지 + 로그인 페이지
    }

    private String registerWithDocs(Model model) throws Exception {
        model.addAttribute("terms", legalService.getActiveDocView("TERMS"));
        model.addAttribute("privacy", legalService.getActiveDocView("PRIVACY"));
        return "user/register";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam(required = false) String token, Model model) throws Exception {
        var r = verifyEmailService.verify(token);
        if (r.success) model.addAttribute("message", r.message);
        else model.addAttribute("error", r.message);
        return "user/verify_email";
    }
}
