package org.zerock.com.example.product;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductDAO productDAO;

    public ProductController(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    @GetMapping
    public String list(
            @RequestParam(name = "category", defaultValue = "mouse") String category,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "new") String sort,
            @RequestParam(defaultValue = "0") int saleOnly,   // ✅ 추가
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request,
            Model model) {

        if (category == null || category.trim().isEmpty()) category = "mouse";
        page = Math.max(1, page);
        size = Math.max(1, size);

        int total = productDAO.countByCategory(category, q, saleOnly == 1);
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) size));
        page = Math.min(page, totalPages);

        int offset = (page - 1) * size;

        List<ProductDTO> list =
                productDAO.listByCategoryPaged(category, q, sort, saleOnly == 1, size, offset);

        model.addAttribute("category", category);
        model.addAttribute("q", q);
        model.addAttribute("sort", sort);
        model.addAttribute("saleOnly", saleOnly);
        model.addAttribute("list", list);

        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("total", total);
        model.addAttribute("totalPages", totalPages);

        // returnUrl 유지
        String uri = request.getRequestURI();
        String qs = request.getQueryString();
        model.addAttribute("returnUrl",
                (qs == null || qs.isBlank()) ? uri : (uri + "?" + qs));

        return "product/list";
    }


}
