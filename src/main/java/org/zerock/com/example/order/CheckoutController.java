package org.zerock.com.example.order;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.com.example.cart.CartDAO;
import org.zerock.com.example.cart.CartItemDTO;
import org.zerock.com.example.pay.kakao.KakaoPayService;
import org.zerock.com.example.pay.kakao.KakaoReadyResponse;
import org.zerock.com.example.user.UserDTO;

import java.util.List;

@Controller
@RequestMapping("/order")
public class CheckoutController {

    private final CartDAO cartDAO;
    private final CheckoutService checkoutService;
    private final KakaoPayService kakaoPayService;
    private final OrderDAO orderDAO;

    public CheckoutController(CartDAO cartDAO,
                              CheckoutService checkoutService,
                              KakaoPayService kakaoPayService,
                              OrderDAO orderDAO) {
        this.cartDAO = cartDAO;
        this.checkoutService = checkoutService;
        this.kakaoPayService = kakaoPayService;
        this.orderDAO = orderDAO;
    }

    @GetMapping("/checkout")
    public String checkoutPage(HttpSession session, Model model) throws Exception {
        UserDTO user = (UserDTO) session.getAttribute("LOGIN_USER");
        if (user == null) return "redirect:/login";

        List<CartItemDTO> cart = cartDAO.list(user.getUserId());
        model.addAttribute("cart", cart);

        long itemsTotal = 0;
        for (CartItemDTO c : cart) itemsTotal += (long) c.getUnitPrice() * c.getQuantity();

        CheckoutSummary s = new CheckoutSummary();
        s.setItemsTotal(itemsTotal);
        s.setDiscountRate(0);
        s.setDiscountAmount(0);
        s.setShippingFee(0);
        s.setFinalTotal(itemsTotal);
        s.setPointRate(0);
        s.setEarnPoint(0);

        model.addAttribute("summary", s);
        return "order/checkout";
    }

    @PostMapping("/checkout")
    public String checkoutSubmit(@RequestParam String address,
                                 @RequestParam String payMethod,
                                 HttpSession session,
                                 Model model) throws Exception {

        UserDTO user = (UserDTO) session.getAttribute("LOGIN_USER");
        if (user == null) return "redirect:/login";

        if ("KAKAO".equalsIgnoreCase(payMethod)) {
            String redirectUrl = checkoutService.startKakaoReady(user.getUserId(), address);
            return "redirect:" + redirectUrl;
        }

        // 나머지(무통장/가정결제)는 기존 올인원
        OrderResult r = checkoutService.checkoutAllInOne(user.getUserId(), address, payMethod);

        session.setAttribute("LOGIN_USER", checkoutService.reloadUser(user.getUserId()));
        model.addAttribute("result", r);
        return "order/result";
    }

}
