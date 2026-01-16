package org.zerock.com.example.pay.kakao;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.com.example.common.DBUtil;
import org.zerock.com.example.order.CheckoutService;
import org.zerock.com.example.order.OrderDAO;
import org.zerock.com.example.order.OrderPayInfo;
import org.zerock.com.example.user.UserDTO;

import java.sql.Connection;

@Controller
public class KakaoPayController {

    private final KakaoPayService kakaoPayService;
    private final CheckoutService checkoutService;
    private final OrderDAO orderDAO;

    public KakaoPayController(KakaoPayService kakaoPayService,
                              CheckoutService checkoutService,
                              OrderDAO orderDAO) {
        this.kakaoPayService = kakaoPayService;
        this.checkoutService = checkoutService;
        this.orderDAO = orderDAO;
    }

    // ✅ 성공 콜백
    @GetMapping("/pay/kakao/approve")
    public String approve(@RequestParam("orderNo") String orderNo,
                          @RequestParam("pg_token") String pgToken,
                          HttpSession session) throws Exception {

        UserDTO user = (UserDTO) session.getAttribute("LOGIN_USER");
        if (user == null) return "redirect:/login";

        OrderPayInfo info;
        try (Connection con = DBUtil.getConnection()) {
            info = orderDAO.findPayInfoByOrderNoAndUser(con, orderNo, user.getUserId());
        }

        if (info == null) return "redirect:/order/checkout?msg=order_not_found";
        if (info.getKakaoTid() == null || info.getKakaoTid().isBlank())
            return "redirect:/order/checkout?msg=tid_missing";

        KakaoApproveResponse approved = kakaoPayService.approve(
                info.getKakaoTid(),
                info.getOrderNo(),
                String.valueOf(user.getUserId()),
                pgToken
        );

        checkoutService.verifyAmount(info.getTotalPrice(), approved);
        checkoutService.finalizePaidAfterKakaoApprove(user.getUserId(), info.getOrderId());

        session.setAttribute("LOGIN_USER", checkoutService.reloadUser(user.getUserId()));
        return "redirect:/order/result?orderId=" + info.getOrderId();
    }
    @GetMapping("/pay/kakao/cancel")
    public String cancel(@RequestParam("orderNo") String orderNo,
                         Model model) {
        model.addAttribute("orderNo", orderNo);
        return "pay/kakao_cancel";
    }

    @GetMapping("/pay/kakao/fail")
    public String fail(@RequestParam String orderNo, Model model) {
        long now = System.currentTimeMillis();
        System.out.println("[KAKAO][FAIL] orderNo=" + orderNo + " at=" + now);
        model.addAttribute("orderNo", orderNo);
        return "pay/kakao_fail";
    }



}
