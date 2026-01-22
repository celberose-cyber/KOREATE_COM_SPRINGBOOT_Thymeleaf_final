package org.zerock.com.example.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.com.example.legal.LegalService;

import java.util.Map;

@Controller
public class AuthController {

    private final UserDAO userDAO;
    private final RegisterService registerService;
    private final LegalService legalService;
    private final VerifyEmailService verifyEmailService;

    private final EmailCodeService emailCodeService;

    private final PasswordResetService passwordResetService;

    public AuthController(UserDAO userDAO,
                          RegisterService registerService,
                          LegalService legalService,
                          VerifyEmailService verifyEmailService,
                          EmailCodeService emailCodeService,
                          PasswordResetService passwordResetService) {
        this.userDAO = userDAO;
        this.registerService = registerService;
        this.legalService = legalService;
        this.verifyEmailService = verifyEmailService;
        this.emailCodeService = emailCodeService;
        this.passwordResetService = passwordResetService;
    }


    @GetMapping("/login")
    public String loginForm(HttpSession session, Model model) {
        String msg = (String) session.getAttribute("FLASH_MESSAGE");
        if (msg != null) {
            session.removeAttribute("FLASH_MESSAGE");
            model.addAttribute("message", msg);
        }
        return "user/login";
    }

    @PostMapping("/login")
    public String loginSubmit(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model
    ) throws Exception {

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            model.addAttribute("error", "아이디/비밀번호를 입력해주세요.");
            return "user/login";
        }

        String u = username.trim().toLowerCase();
        String hash = PasswordUtil.sha256(password);

        UserDTO user = userDAO.findByUsernameAndHash(u, hash);

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
        model.addAttribute("terms", legalService.getActiveDocView("TERMS"));
        model.addAttribute("privacy", legalService.getActiveDocView("PRIVACY"));
        return "user/register";
    }
    @PostMapping("/register")
    public String registerSubmit(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String name,
            @RequestParam String phone,
            @RequestParam String verifyEmail,

            @RequestParam(required = false) String termsAgreed,
            @RequestParam(required = false) String privacyAgreed,
            @RequestParam(required = false) Long termsDocId,
            @RequestParam(required = false) Long privacyDocId,

            HttpServletRequest req,
            Model model
    ) throws Exception {

        // ✅ 입력값 유지용(에러 시 다시 보여주기)
        model.addAttribute("f_username", username);
        model.addAttribute("f_name", name);
        model.addAttribute("f_phone", phone);
        model.addAttribute("f_verifyEmail", verifyEmail);
        model.addAttribute("f_termsAgreed", termsAgreed != null);
        model.addAttribute("f_privacyAgreed", privacyAgreed != null);

        if (username == null || username.isBlank()
                || password == null || password.isBlank()
                || name == null || name.isBlank()
                || phone == null || phone.isBlank()
                || verifyEmail == null || verifyEmail.isBlank()) {
            model.addAttribute("error", "모든 항목을 입력해주세요.");
            return registerWithDocs(model);
        }

        if (termsAgreed == null || privacyAgreed == null) {
            model.addAttribute("error", "이용약관/개인정보처리방침에 동의해야 가입할 수 있습니다.");
            return registerWithDocs(model);
        }
        if (!isValidPassword(password)) {
            model.addAttribute("error", "비밀번호는 영문과 숫자를 포함한 6자리 이상이어야 합니다.");
            return registerWithDocs(model);
        }
        final String purpose = "REGISTER";

        HttpSession session = req.getSession();
        Object okObj = session.getAttribute(purpose + "_EMAIL_VERIFIED");

        boolean ok = (okObj instanceof Boolean b) ? b
                : (okObj instanceof String s) && Boolean.parseBoolean(s);

        String inputEmail = verifyEmail.trim().toLowerCase();
        String verifiedEmail = (String) session.getAttribute(purpose + "_EMAIL");

        if (!ok || verifiedEmail == null || !verifiedEmail.equals(inputEmail)) {
            model.addAttribute("error", "이메일 인증을 먼저 완료해주세요.");
            return registerWithDocs(model);
        }

        RegisterService.RegisterCommand cmd = new RegisterService.RegisterCommand();
        cmd.username = username.trim().toLowerCase();
        cmd.verifyEmail = verifyEmail.trim().toLowerCase();
        cmd.passwordHash = PasswordUtil.sha256(password);
        cmd.name = name;
        cmd.phone = phone;
        cmd.ipAddr = req.getRemoteAddr();
        cmd.userAgent = req.getHeader("User-Agent");
        cmd.termsDocId = termsDocId;
        cmd.privacyDocId = privacyDocId;

        var result = registerService.register(cmd);

        if (!result.success) {
            model.addAttribute("error", result.message);
            return registerWithDocs(model);
        }

        // ✅ 가입 성공 시: 세션 플래그 제거
        session.removeAttribute(purpose + "_EMAIL_VERIFIED");
        session.removeAttribute(purpose + "_EMAIL");


        // ✅ 가입완료 안내 → 로그인 페이지로 이동(redirect + flash)
        session.setAttribute("FLASH_MESSAGE", "축하합니다! 회원가입이 완료되었습니다. 로그인해주세요.");
        return "redirect:/register_complete";
    }

    @GetMapping("/register_complete")
    public String registerComplete(HttpSession session, Model model) {
        String msg = (String) session.getAttribute("FLASH_MESSAGE");
        session.removeAttribute("FLASH_MESSAGE");
        model.addAttribute("message", msg != null ? msg : "회원가입이 완료되었습니다.");
        return "user/register_complete";
    }

    private String registerWithDocs(Model model) throws Exception {
        model.addAttribute("terms", legalService.getActiveDocView("TERMS"));
        model.addAttribute("privacy", legalService.getActiveDocView("PRIVACY"));
        return "user/register";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam(required = false) String token, Model model) throws Exception {
        var r = verifyEmailService.verify(token);
        if (r.success) model.addAttribute("message", r.message);
        else model.addAttribute("error", r.message);
        return "user/verify_email";
    }
    @PostMapping("/api/email/send-code")
    @ResponseBody
    public Map<String, Object> sendCode(@RequestParam String verifyEmail,
                                        @RequestParam(defaultValue="REGISTER") String purpose) throws Exception {

        var r = emailCodeService.sendCode(verifyEmail, purpose);
        return Map.of("success", r.success, "message", r.message);
    }

    @PostMapping("/api/email/verify-code")
    @ResponseBody
    public Map<String, Object> verifyCode(@RequestParam String verifyEmail,
                                          @RequestParam String code,
                                          @RequestParam(defaultValue="REGISTER") String purpose,
                                          HttpSession session )  throws Exception {
        var r = emailCodeService.verifyCode(verifyEmail, code, purpose);

        if (r.success) {
            session.setAttribute(purpose + "_EMAIL_VERIFIED", Boolean.TRUE);
            session.setAttribute(purpose + "_EMAIL", verifyEmail.trim().toLowerCase());
        }
        return Map.of("success", r.success, "message", r.message);
    }
    @GetMapping("/find-id")
    public String findIdForm() { return "user/find_id"; }

    @GetMapping("/find-id/result")
    public String findIdResult(HttpSession session, Model model) {
        Integer count = (Integer) session.getAttribute("FIND_ID_COUNT");
        session.removeAttribute("FIND_ID_COUNT");

        int c = (count == null) ? 0 : count;
        model.addAttribute("count", c);
        return "user/find_id_result";
    }

    @GetMapping("/reset-password")
    public String resetPwForm() { return "user/reset_pw"; }

    @GetMapping("/reset-password/new")
    public String resetPwNewForm(@RequestParam(required = false) String token,
                                 HttpSession session,
                                 Model model) {

        if (token == null || token.isBlank()) {
            session.setAttribute("FLASH_MESSAGE", "비밀번호 재설정 링크가 올바르지 않습니다. 다시 시도해주세요.");
            return "redirect:/reset-password";
        }

        model.addAttribute("token", token);
        return "user/reset_pw_new";
    }

    @PostMapping("/reset-password/new")
    public String resetPwNewSubmit(@RequestParam String token,
                                   @RequestParam String newPassword,
                                   @RequestParam String newPassword2,
                                   HttpSession session,
                                   Model model) throws Exception {

        if (!newPassword.equals(newPassword2)) {
            model.addAttribute("error", "비밀번호가 서로 일치하지 않습니다.");
            model.addAttribute("token", token);
            return "user/reset_pw_new";
        }

        if (!isValidPassword(newPassword)) {
            model.addAttribute("error", "비밀번호는 영문과 숫자를 포함한 6자리 이상이어야 합니다.");
            model.addAttribute("token", token);
            return "user/reset_pw_new";
        }

        var r = passwordResetService.resetPassword(token, newPassword);
        if (!r.success) {
            model.addAttribute("error", r.message);
            model.addAttribute("token", token);
            return "user/reset_pw_new";
        }

        // ✅ 로그인 화면에서 메시지 보여주기
        session.setAttribute("FLASH_MESSAGE", "비밀번호 변경이 완료되었습니다. 로그인해주세요.");
        return "redirect:/login";
    }

    @PostMapping("/find-id")
    public String findIdSubmit(@RequestParam String name,
                               @RequestParam String verifyEmail,
                               HttpSession session,
                               Model model) throws Exception {

        final String purpose = "FIND_ID";
        boolean ok = Boolean.TRUE.equals(session.getAttribute(purpose + "_EMAIL_VERIFIED"));
        String verifiedEmail = (String) session.getAttribute(purpose + "_EMAIL");
        String inputEmail = verifyEmail.trim().toLowerCase();

        if (!ok || verifiedEmail == null || !verifiedEmail.equals(inputEmail))  {
            model.addAttribute("error", "이메일 인증을 먼저 완료해주세요.");
            model.addAttribute("f_name", name);
            model.addAttribute("f_verifyEmail", verifyEmail);
            return "user/find_id";
        }

        // 2) username 목록 조회
        var usernames = userDAO.findUsernamesByNameAndEmail(name, inputEmail);

        // 3) 세션 정리(인증 플래그 제거)
        session.removeAttribute("FIND_ID_EMAIL_VERIFIED");
        session.removeAttribute("FIND_ID_EMAIL");

        // 4) 결과 정책: 화면에는 count만, 실제 목록은 이메일로
        int count = (usernames == null) ? 0 : usernames.size();

        if (count > 0) {
            // ✅ 이메일로만 아이디 목록 전달
            emailCodeService.sendFoundUsernamesEmail(inputEmail, usernames);
        }

        // ✅ 화면용 메시지(개수만)
        session.setAttribute("FIND_ID_COUNT", count);
        return "redirect:/find-id/result";
    }

    @PostMapping("/reset-password")
    public String resetPwSubmit(@RequestParam String username,
                                @RequestParam String verifyEmail,
                                HttpSession session,
                                Model model) throws Exception {

        final String purpose = "RESET_PW";

        boolean ok = Boolean.TRUE.equals(session.getAttribute(purpose + "_EMAIL_VERIFIED"));
        String verifiedEmail = (String) session.getAttribute(purpose + "_EMAIL");

        String inputEmail = verifyEmail.trim().toLowerCase();

        if (!ok || verifiedEmail == null || !verifiedEmail.equals(inputEmail)) {
            model.addAttribute("error", "이메일 인증을 먼저 완료해주세요.");
            model.addAttribute("f_username", username);
            model.addAttribute("f_verifyEmail", verifyEmail);
            return "user/reset_pw";
        }

// ✅ 검증 통과 후에만 실행
        var r = passwordResetService.issueResetToken(username, inputEmail);



        // 세션 정리
        session.removeAttribute(purpose + "_EMAIL_VERIFIED");
        session.removeAttribute(purpose + "_EMAIL");

        if (!r.success) {
            model.addAttribute("error", r.message);
            return "user/reset_pw";
        }

        return "redirect:/reset-password/new?token=" + r.token;
    }

    @PostMapping("/api/user/check-username")
    @ResponseBody
    public Map<String, Object> checkUsername(@RequestParam String username) throws Exception {
        if (username == null || username.isBlank()) {
            return Map.of("success", false, "available", false, "message", "아이디를 입력해주세요.");
        }

        String u = username.trim().toLowerCase();

        // 아이디 형식 간단 검증(선택)
        if (!u.matches("^[a-z0-9_]{4,20}$")) {
            return Map.of("success", true, "available", false,
                    "message", "아이디는 4~20자, 영문 소문자/숫자/_ 만 허용합니다.");
        }

        boolean exists = userDAO.existsByUsername(u);  // 아래 DAO 추가/연결
        if (exists) {
            return Map.of("success", true, "available", false, "message", "이미 사용 중인 아이디입니다.");
        }
        return Map.of("success", true, "available", true, "message", "사용 가능한 아이디입니다.");
    }

    private boolean isValidPassword(String pw) {
        if (pw == null) return false;
        // 최소 6자리, 영문 1개 이상, 숫자 1개 이상
        return pw.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$");
    }

}
