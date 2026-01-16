package org.zerock.com.example.order;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.com.example.common.DBUtil;
import org.zerock.com.example.user.UserDTO;

import java.sql.Connection;
import java.util.List;

@Controller
@RequestMapping("/my/orders")
public class MyOrderController {

    private final OrderDAO orderDAO;
    private final CheckoutService checkoutService;

    public MyOrderController(OrderDAO orderDAO, CheckoutService checkoutService) {
        this.orderDAO = orderDAO;
        this.checkoutService = checkoutService;
    }

    // ✅ 주문 목록
    @GetMapping
    public String list(HttpSession session, Model model) throws Exception {
        UserDTO user = (UserDTO) session.getAttribute("LOGIN_USER");
        if (user == null) return "redirect:/login";

        try (Connection con = DBUtil.getConnection()) {
            model.addAttribute("orders",
                    orderDAO.listByUser(con, user.getUserId()));
        }
        return "order/list";
    }

    // ✅ 주문 상세 (핵심 수정)
    @GetMapping("/{orderId}")
    public String detail(@PathVariable long orderId,
                         HttpSession session,
                         Model model) throws Exception {

        UserDTO user = (UserDTO) session.getAttribute("LOGIN_USER");
        if (user == null) return "redirect:/login";

        try (Connection con = DBUtil.getConnection()) {

            OrderDetailHeader header =
                    orderDAO.findOrderHeader(con, orderId, user.getUserId());

            if (header == null) {
                return "redirect:/my/orders";
            }

            List<OrderDetailItem> items =
                    orderDAO.findOrderItems(con, orderId);

            model.addAttribute("header", header);
            model.addAttribute("items", items);
        }

        return "order/detail";
    }

    // 구매확정
    @PostMapping("/{orderId}/confirm")
    public String confirm(@PathVariable long orderId,
                          HttpSession session) throws Exception {
        UserDTO user = (UserDTO) session.getAttribute("LOGIN_USER");
        if (user == null) return "redirect:/login";

        checkoutService.confirmPurchase(user.getUserId(), orderId);
        return "redirect:/my/orders?msg=confirmed";
    }

    // 취소요청
    @PostMapping("/{orderId}/cancel-request")
    public String cancelRequest(@PathVariable long orderId,
                                @RequestParam(required = false) String reason,
                                HttpSession session) throws Exception {
        UserDTO user = (UserDTO) session.getAttribute("LOGIN_USER");
        if (user == null) return "redirect:/login";

        checkoutService.requestCancel(user.getUserId(), orderId, reason);
        return "redirect:/my/orders?msg=cancel_requested";
    }
}

