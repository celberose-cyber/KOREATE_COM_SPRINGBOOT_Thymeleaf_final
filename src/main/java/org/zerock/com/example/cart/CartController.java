package org.zerock.com.example.cart;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.zerock.com.example.product.ProductDAO;
import org.zerock.com.example.product.ProductDTO;
import org.zerock.com.example.user.UserDTO;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartDAO cartDAO;
    private final ProductDAO productDAO;
    private final CartService cartService;

    public CartController(CartDAO cartDAO, ProductDAO productDAO, CartService cartService) {
        this.cartDAO = cartDAO;
        this.productDAO = productDAO;
        this.cartService = cartService;
    }

    @GetMapping
    public String cartPage(@RequestParam(required = false) String returnUrl,
                           HttpSession session,
                           Model model) throws Exception {

        UserDTO user = (UserDTO) session.getAttribute("LOGIN_USER");
        if (user == null) return "redirect:/login";

        List<CartItemDTO> items = cartDAO.list(user.getUserId());
        model.addAttribute("items", items);
        model.addAttribute("returnUrl", returnUrl);

        CartSummaryDTO summary = cartService.summarize(items, user.getTotalSpent());

        // 기존 템플릿에서 쓰던 model 키 유지
        model.addAttribute("total", summary.getTotal());
        model.addAttribute("discountAmount", summary.getDiscountAmount());
        model.addAttribute("discountedTotal", summary.getDiscountedTotal());
        model.addAttribute("expectedPoint", summary.getExpectedPoint());
        model.addAttribute("policy", summary.getPolicy());

        // 혹시 화면에서 discountRate/pointRate를 직접 쓰고 있다면 아래도 유지
        if (summary.getPolicy() != null) {
            model.addAttribute("discountRate",
                    summary.getPolicy().getDiscountRate() == null ? java.math.BigDecimal.ZERO : summary.getPolicy().getDiscountRate());
            model.addAttribute("pointRate",
                    summary.getPolicy().getPointRate() == null ? java.math.BigDecimal.ZERO : summary.getPolicy().getPointRate());
        } else {
            model.addAttribute("discountRate", java.math.BigDecimal.ZERO);
            model.addAttribute("pointRate", java.math.BigDecimal.ZERO);
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

        int safeQty = (qty == null) ? 1 : Math.max(1, qty);

        switch (action) {
            case "add" -> {
                if (productId == null) return "redirect:/cart";

                ProductDTO p = productDAO.findById(productId);
                if (p == null) {
                    ra.addFlashAttribute("toast", "상품을 찾을 수 없습니다.");
                    return safeRedirect(returnUrl, "/products");
                }

                long unitPrice = p.getEffectivePrice();   // ✅ 핵심 교체
                cartDAO.addOrIncrease(user.getUserId(), productId, safeQty, unitPrice);

                ra.addFlashAttribute("toast", p.getName() + " 장바구니에 담겼습니다.");
                return safeRedirect(returnUrl, "/products");
            }
            case "update" -> {
                if (cartItemId != null) cartDAO.updateQty(user.getUserId(), cartItemId, safeQty);
                ra.addFlashAttribute("toast", "수량이 변경되었습니다.");
                return "redirect:/cart";
            }
            case "delete" -> {
                if (cartItemId != null) cartDAO.delete(user.getUserId(), cartItemId);
                ra.addFlashAttribute("toast", "삭제되었습니다.");
                return "redirect:/cart";
            }
            case "clear" -> {
                cartDAO.clear(user.getUserId());
                ra.addFlashAttribute("toast", "장바구니를 비웠습니다.");
                return "redirect:/cart";
            }
            case "refreshPrice" -> {
                if (cartItemId == null) return "redirect:/cart";

                // 1) cartItem 조회해서 productId 얻기 (메서드 하나 추가 필요)
                CartItemDTO it = cartDAO.findByCartItemId(user.getUserId(), cartItemId);
                if (it == null) {
                    ra.addFlashAttribute("toast", "장바구니 항목을 찾을 수 없습니다.");
                    return "redirect:/cart";
                }

                // 2) 현재 상품 가격 계산해서 cart_items.unit_price를 갱신
                ProductDTO p = productDAO.findById(it.getProductId());
                if (p == null) {
                    ra.addFlashAttribute("toast", "상품 정보를 찾을 수 없습니다.");
                    return "redirect:/cart";
                }

                long newUnitPrice = p.getEffectivePrice();
                cartDAO.updateUnitPrice(user.getUserId(), cartItemId, newUnitPrice);

                ra.addFlashAttribute("toast", "최신 가격으로 업데이트했습니다.");
                return "redirect:/cart";
            }

            default -> {
                return "redirect:/cart";
            }

        }
    }

    private String safeRedirect(String returnUrl, String fallback) {
        if (returnUrl != null && returnUrl.startsWith("/")) {
            return "redirect:" + returnUrl;
        }
        return "redirect:" + fallback;
    }
}
