package org.zerock.com.example.order;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.com.example.common.DBUtil;
import org.zerock.com.example.user.UserDTO;

import java.sql.Connection;

@Controller
@RequestMapping("/order")
public class OrderResultController {

    private final OrderDAO orderDAO;

    public OrderResultController(OrderDAO orderDAO) {
        this.orderDAO = orderDAO;
    }

    @GetMapping("/result")
    public String result(@RequestParam("orderId") long orderId,
                         Model model,
                         HttpSession session) throws Exception {

        UserDTO user = (UserDTO) session.getAttribute("LOGIN_USER");
        if (user == null) return "redirect:/login";

        OrderResult v;
        try (Connection con = DBUtil.getConnection()) {
            v = orderDAO.findResultByOrderIdAndUser(con, orderId, user.getUserId());
        }
        if (v == null) return "redirect:/order/checkout?msg=not_your_order";

        model.addAttribute("result", v);
        return "order/result";
    }

}
