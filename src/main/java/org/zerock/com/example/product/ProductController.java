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
    public String list(@RequestParam(name = "category", defaultValue = "mouse") String category,
                       @RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "new") String sort,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size,
                       HttpServletRequest request,
                       Model model) {

        if (category == null || category.trim().isEmpty()) category = "mouse";
        if (page < 1) page = 1;
        if (size < 1) size = 20;

        int total = productDAO.countByCategory(category, q);
        int totalPages = (int) Math.ceil(total / (double) size);

        if (totalPages == 0) totalPages = 1;         // 결과 0개일 때도 UI 안정화
        if (page > totalPages) page = totalPages;

        int offset = (page - 1) * size;
        List<ProductDTO> list = productDAO.listByCategoryPaged(category, q, sort, size, offset);

        model.addAttribute("category", category);
        model.addAttribute("q", q);
        model.addAttribute("sort", sort);
        model.addAttribute("list", list);

        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("total", total);
        model.addAttribute("totalPages", totalPages);

        // returnUrl 유지
        String uri = request.getRequestURI();
        String qs  = request.getQueryString();
        String returnUrl = (qs == null || qs.isBlank()) ? uri : (uri + "?" + qs);
        model.addAttribute("returnUrl", returnUrl);

        return "product/list";
    }

}
