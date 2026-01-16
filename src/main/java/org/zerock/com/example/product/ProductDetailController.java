package org.zerock.com.example.product;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProductDetailController {

    private final ProductDAO productDAO;

    public ProductDetailController(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    @GetMapping("/product")
    public String detail(@RequestParam("id") long id,
                         @RequestParam(value = "returnUrl", required = false) String returnUrl,
                         Model model) throws Exception {

        ProductDTO p = productDAO.findById(id);
        if (p == null) {
            // 상품 없으면 목록으로
            return "redirect:/products";
        }

        model.addAttribute("p", p);
        model.addAttribute("returnUrl", returnUrl); // 상세 템플릿에서 뒤로가기 버튼에 사용
        return "product/detail";
    }
}
