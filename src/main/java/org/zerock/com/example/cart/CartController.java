package org.zerock.com.example.cart;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.zerock.com.example.product.ProductDAO;
import org.zerock.com.example.product.ProductDTO;
import org.zerock.com.example.user.GradePolicyDAO;
import org.zerock.com.example.user.GradePolicyDTO;
import org.zerock.com.example.user.UserDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartDAO cartDAO;
    private final ProductDAO productDAO;

    public CartController(CartDAO cartDAO, ProductDAO productDAO) {
        this.cartDAO = cartDAO;
        this.productDAO = productDAO;
    }

    @GetMapping
    public String cartPage(@RequestParam(required = false) String returnUrl,
                           HttpSession session, Model model) throws Exception {
        UserDTO user = (UserDTO) session.getAttribute("LOGIN_USER");
        if (user == null) return "redirect:/login";

        List<CartItemDTO> items = cartDAO.list(user.getUserId());
        model.addAttribute("items", items);

        // ✅ total을 long으로 통일
        long total = items.stream()
                .mapToLong(i -> i.getUnitPrice() * (long) i.getQuantity())
                .sum();
        model.addAttribute("total", total);

        // returnUrl은 try 밖에서 항상 넣어두는 게 안전
        model.addAttribute("returnUrl", returnUrl);

        try (var con = org.zerock.com.example.common.DBUtil.getConnection()) {

            GradePolicyDAO gpDao = new GradePolicyDAO();
            GradePolicyDTO policy = gpDao.findPolicyByTotalSpent(con, user.getTotalSpent());

            java.math.BigDecimal discountRate =
                    (policy == null || policy.getDiscountRate() == null)
                            ? java.math.BigDecimal.ZERO : policy.getDiscountRate();

            java.math.BigDecimal pointRate =
                    (policy == null || policy.getPointRate() == null)
                            ? java.math.BigDecimal.ZERO : policy.getPointRate();

            model.addAttribute("discountRate", discountRate);
            model.addAttribute("pointRate", pointRate);

            java.math.BigDecimal totalBD = java.math.BigDecimal.valueOf(total);

            java.math.BigDecimal discountAmount = totalBD
                    .multiply(discountRate)
                    .divide(java.math.BigDecimal.valueOf(100), 0, java.math.RoundingMode.DOWN);

            java.math.BigDecimal discountedTotal = totalBD.subtract(discountAmount);

            java.math.BigDecimal expectedPoint = discountedTotal
                    .multiply(pointRate)
                    .divide(java.math.BigDecimal.valueOf(100), 0, java.math.RoundingMode.DOWN);

            // ✅ 화면에는 long으로 내려주기
            model.addAttribute("discountAmount", discountAmount.longValue());
            model.addAttribute("discountedTotal", discountedTotal.longValue());
            model.addAttribute("expectedPoint", expectedPoint.longValue());

            model.addAttribute("policy", policy);
        }

        return "cart/cart";
    }




    @PostMapping
    public String cartAction(@RequestParam String action,
                             @RequestParam(required = false) Long productId,
                             @RequestParam(required = false) Long cartItemId,
                             @RequestParam(required = false, defaultValue = "1") Integer qty,
                             @RequestParam(required = false) String returnUrl,
                             HttpSession session,
                             RedirectAttributes ra) throws Exception {

        UserDTO user = (UserDTO) session.getAttribute("LOGIN_USER");
        if (user == null) return "redirect:/login";

        if ("add".equals(action)) {
            if (productId == null) return "redirect:/cart";
            ProductDTO p = productDAO.findById(productId);
            long unitPrice = p.getPrice();
            cartDAO.addOrIncrease(user.getUserId(), productId, qty, unitPrice);
            ra.addFlashAttribute("toast", p.getName() + " 장바구니에 담겼습니다.");

            // add는 returnUrl 있으면 복귀, 없으면 products
            return safeRedirect(returnUrl, "/products");
        }

        if ("update".equals(action)) {
            if (cartItemId != null) cartDAO.updateQty(user.getUserId(), cartItemId, qty);
            ra.addFlashAttribute("toast", "수량이 변경되었습니다.");
            return "redirect:/cart"; // ✅ 무조건 cart 유지
        }

        if ("delete".equals(action)) {
            if (cartItemId != null) cartDAO.delete(user.getUserId(), cartItemId);
            ra.addFlashAttribute("toast", "삭제되었습니다.");
            return "redirect:/cart"; // ✅
        }

        if ("clear".equals(action)) {
            cartDAO.clear(user.getUserId());
            ra.addFlashAttribute("toast", "장바구니를 비웠습니다.");
            return "redirect:/cart"; // ✅
        }

        return "redirect:/cart";
    }
    private String safeRedirect(String returnUrl, String fallback) {
        if (returnUrl != null && returnUrl.startsWith("/")) {
            return "redirect:" + returnUrl;
        }
        return "redirect:" + fallback;
    }
}
