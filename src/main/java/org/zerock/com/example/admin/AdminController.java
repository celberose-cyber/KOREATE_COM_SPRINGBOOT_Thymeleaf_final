package org.zerock.com.example.admin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.com.example.common.DBUtil;
import org.zerock.com.example.common.NoticeDAO;
import org.zerock.com.example.common.SuggestDAO;
import org.zerock.com.example.order.CheckoutService;
import org.zerock.com.example.order.OrderDAO;
import org.zerock.com.example.product.ProductDAO;
import org.zerock.com.example.user.UserDAO;
import org.zerock.com.example.user.UserDTO;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UserDAO userDAO;
    private final ProductDAO productDAO;
    private final NoticeDAO noticeDAO;
    private final SuggestDAO suggestDAO;
    private final OrderDAO orderDAO;

    private final CheckoutService checkoutService;

    private final AdminMemberService adminMemberService;

    public AdminController(UserDAO userDAO, ProductDAO productDAO, NoticeDAO noticeDAO,
                           SuggestDAO suggestDAO, OrderDAO orderDAO,
                           CheckoutService checkoutService,
                           AdminMemberService adminMemberService) {
        this.userDAO = userDAO;
        this.productDAO = productDAO;
        this.noticeDAO = noticeDAO;
        this.suggestDAO = suggestDAO;
        this.orderDAO = orderDAO;
        this.checkoutService = checkoutService;
        this.adminMemberService = adminMemberService;
    }


    private boolean isAdmin(HttpSession session) {
        Object obj = session.getAttribute("LOGIN_USER");
        if (!(obj instanceof UserDTO u)) return false;
        return "ADMIN".equalsIgnoreCase(u.getRole());
    }

    @GetMapping("/members")
    public String members(
            @RequestParam(required=false) String q,
            @RequestParam(defaultValue="idDesc") String sort,
            @RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="20") int size,
            HttpSession session,
            Model model
    ) throws Exception {
        if (!isAdmin(session)) return "redirect:/login";

        int p = Math.max(1, page);
        int s = Math.min(100, Math.max(5, size));
        int offset = (p - 1) * s;

        String baseUrl = "/admin/members";
        model.addAttribute("title", "회원 관리");
        model.addAttribute("baseUrl", baseUrl);

        int total = userDAO.countAll(q);
        int lastPage = (int) Math.ceil(total / (double) s);

        model.addAttribute("list", userDAO.listPaged(q, sort, s, offset));

        model.addAttribute("q", q);
        model.addAttribute("sort", sort);
        model.addAttribute("page", p);
        model.addAttribute("size", s);
        model.addAttribute("total", total);
        model.addAttribute("lastPage", Math.max(1, lastPage));

        java.util.Map<String, Object> params = new java.util.LinkedHashMap<>();
        params.put("q", q);
        params.put("sort", sort);
        params.put("size", s);
        model.addAttribute("params", params);

        return "admin/members_list";
    }


    @GetMapping("/members/{id}")
    public String memberDetail(@PathVariable long id, HttpSession session, Model model) throws Exception {
        if (!isAdmin(session)) return "redirect:/login";

        UserDTO u = userDAO.findUserById(id);
        if (u == null) {
            session.setAttribute("FLASH_MESSAGE", "존재하지 않는 회원입니다.");
            return "redirect:/admin/members";
        }

        model.addAttribute("title","회원 상세/수정");
        model.addAttribute("u", u);
        return "admin/member_edit";
    }


    @PostMapping("/members/{id}")
    public String memberUpdate(@PathVariable long id,
                               @RequestParam String name,
                               @RequestParam String phone,
                               @RequestParam String role,
                               @RequestParam String grade,
                               HttpSession session) throws Exception {
        if (!isAdmin(session)) return "redirect:/login";
        userDAO.updateUserAdmin(id, name, phone, role, grade);
        session.setAttribute("FLASH_MESSAGE", "회원 정보가 수정되었습니다.");
        return "redirect:/admin/members/" + id;
    }
    @GetMapping("/products")
    public String adminProducts(
            @RequestParam(defaultValue="all") String category,
            @RequestParam(required=false) String q,
            @RequestParam(defaultValue="updated") String sort,
            @RequestParam(defaultValue="0") int saleOnly,
            @RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="20") int size,
            HttpSession session,
            HttpServletRequest request,
            Model model
    ) {
        if (!isAdmin(session)) return "redirect:/login";

        int p = Math.max(1, page);
        int s = Math.min(100, Math.max(5, size));
        int offset = (p - 1) * s;

        String baseUrl = "/admin/products";
        model.addAttribute("title", "상품 관리");
        model.addAttribute("baseUrl", baseUrl);

        int total = productDAO.countAll(category, q, saleOnly == 1);
        int lastPage = (int)Math.ceil(total / (double)s);

        model.addAttribute("list",
                productDAO.listAllPaged(category, q, sort, saleOnly == 1, s, offset)
        );

        model.addAttribute("category", category);
        model.addAttribute("q", q);
        model.addAttribute("sort", sort);
        model.addAttribute("saleOnly", saleOnly);
        model.addAttribute("page", p);
        model.addAttribute("size", s);
        model.addAttribute("total", total);
        model.addAttribute("lastPage", Math.max(1, lastPage));

        java.util.Map<String, Object> params = new java.util.LinkedHashMap<>();
        params.put("category", category);
        params.put("q", q);
        params.put("sort", sort);
        params.put("saleOnly", saleOnly);
        params.put("size", s);
        model.addAttribute("params", params);

        // ✅ 현재 페이지 URL(+쿼리) = returnUrl
        String qs = request.getQueryString();
        String returnUrl = request.getRequestURI() + (qs != null ? "?" + qs : "");
        model.addAttribute("returnUrl", returnUrl);

        return "admin/products_list";
    }


    @PostMapping("/products/{id}/price")
    public String adminProductUpdatePrice(
            @PathVariable long id,
            @RequestParam long price,
            @RequestParam(required = false) Long salePrice,
            @RequestParam(required = false) String returnUrl,
            HttpSession session
    ) throws Exception {
        if (!isAdmin(session)) return "redirect:/login";

        long newPrice = Math.max(0, price);
        Long newSalePrice = (salePrice == null) ? null : Math.max(0, salePrice);

        productDAO.updatePrices(id, newPrice, newSalePrice);
        session.setAttribute("FLASH_MESSAGE", "가격이 변경되었습니다.");

        return (returnUrl != null && !returnUrl.isBlank()) ? "redirect:" + returnUrl : "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/sale")
    public String adminProductUpdateSale(
            @PathVariable long id,
            @RequestParam int onSale,
            @RequestParam(required = false) Long salePrice,
            @RequestParam(required = false) String returnUrl,
            HttpSession session
    ) throws Exception {
        if (!isAdmin(session)) return "redirect:/login";

        boolean isOnSale = (onSale == 1);
        Long newSalePrice = (salePrice == null) ? null : Math.max(0, salePrice);

        // ✅ salePrice를 안 보내면 "가격을 건드리지 않고 토글만" 하도록 DAO에서 처리
        productDAO.updateSaleStatus(id, isOnSale, newSalePrice);

        session.setAttribute("FLASH_MESSAGE", "세일 상태가 변경되었습니다.");
        return (returnUrl != null && !returnUrl.isBlank()) ? "redirect:" + returnUrl : "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String adminProductDelete(
            @PathVariable long id,
            @RequestParam(required = false) String returnUrl,
            HttpSession session
    ) throws Exception {
        if (!isAdmin(session)) return "redirect:/login";

        productDAO.deleteById(id);
        session.setAttribute("FLASH_MESSAGE", "상품이 삭제되었습니다.");

        return (returnUrl != null && !returnUrl.isBlank()) ? "redirect:" + returnUrl : "redirect:/admin/products";
    }


    @GetMapping
    public String dashboard(
            @RequestParam(required=false) String start,  // yyyy-MM-dd
            @RequestParam(required=false) String end,    // yyyy-MM-dd
            @RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="10") int size,
            HttpSession session,
            Model model
    ) throws Exception {
        if (!isAdmin(session)) return "redirect:/login";

        java.time.LocalDate to = (end == null || end.isBlank())
                ? java.time.LocalDate.now()
                : java.time.LocalDate.parse(end);

        java.time.LocalDate from = (start == null || start.isBlank())
                ? to.minusDays(29)   // 기본 30일
                : java.time.LocalDate.parse(start);

        int p = Math.max(1, page);
        int s = Math.min(50, Math.max(5, size)); // 대시보드는 5~50 정도면 충분
        int offset = (p - 1) * s;

        String baseUrl = "/admin";
        model.addAttribute("title", "관리자 대시보드");
        model.addAttribute("baseUrl", baseUrl);

        model.addAttribute("start", from.toString());
        model.addAttribute("end", to.toString());
        model.addAttribute("page", p);
        model.addAttribute("size", s);

        // params: paging fragment로 넘길 쿼리 유지용
        java.util.Map<String, Object> params = new java.util.LinkedHashMap<>();
        params.put("start", from.toString());
        params.put("end", to.toString());
        params.put("size", s);
        model.addAttribute("params", params);

        try (java.sql.Connection con = org.zerock.com.example.common.DBUtil.getConnection()) {

            int total = orderDAO.countDailySalesPaidAt(con, from, to);
            int lastPage = (int) Math.ceil(total / (double) s);

            model.addAttribute("total", total);
            model.addAttribute("lastPage", Math.max(1, lastPage));

            model.addAttribute("daily",
                    orderDAO.listDailySalesPaidAtPaged(con, from, to, s, offset)
            );
        }

        return "admin/dashboard";
    }

    @GetMapping("/orders")
    public String adminOrders(
            @RequestParam(required=false) Long userId,
            @RequestParam(required=false) String username,
            @RequestParam(required=false) String start,
            @RequestParam(required=false) String end,
            @RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="20") int size,
            HttpSession session,
            Model model
    ) throws Exception {

        if (!isAdmin(session)) return "redirect:/login";

        // ✅ 날짜 기본값: 최근 7일 (원하면 30일로 바꿔도 OK)
        LocalDate to = (end == null || end.isBlank())
                ? LocalDate.now()
                : LocalDate.parse(end);

        LocalDate from = (start == null || start.isBlank())
                ? to.minusDays(7)
                : LocalDate.parse(start);

        int p = Math.max(1, page);
        int s = Math.min(100, Math.max(5, size));   // ✅ size 방어 (5~100)
        int offset = (p - 1) * s;

        Long resolvedUserId = userId;  // ✅ 최종 필터용
        String filterName = null;      // ✅ 화면 표시용 (username(#id))
        String filterError = null;     // ✅ (선택) 에러 메시지

        try (Connection con = DBUtil.getConnection()) {

            // ✅ username → userId 변환 (userId가 없고 username이 있을 때만)
            if (resolvedUserId == null && username != null && !username.isBlank()) {
                try {
                    resolvedUserId = userDAO.findIdByUsername(con, username.trim());

                    if (resolvedUserId == null) {
                        // username이 없으면: "전체목록"은 유지하면서 에러만 표시
                        filterError = "해당 username을 찾을 수 없습니다: " + username.trim();
                    }
                } catch (Exception e) {
                    // ✅ 변환 실패해도 전체목록은 살아야 함
                    filterError = "username 조회 오류: " + e.getMessage();
                    resolvedUserId = null;
                }
            }

            // ✅ 표시용 이름 세팅 (필터가 실제로 적용될 때만)
            if (resolvedUserId != null) {
                String uname = null;
                try { uname = userDAO.findUsernameById(con, resolvedUserId); } catch (Exception ignore) {}
                filterName = (uname != null && !uname.isBlank())
                        ? (uname + "(#" + resolvedUserId + ")")
                        : ("#" + resolvedUserId);
            }

            int total = orderDAO.countForAdmin(con, resolvedUserId, from, to);
            int lastPage = (int) Math.ceil(total / (double) s);

            model.addAttribute("orders",
                    orderDAO.listForAdminPaged(con, resolvedUserId, from, to, s, offset));

            model.addAttribute("total", total);
            model.addAttribute("lastPage", Math.max(1, lastPage));
        }

        model.addAttribute("title", "주문 관리");
        model.addAttribute("baseUrl", "/admin/orders");

        // ✅ 입력값 유지
        model.addAttribute("userId", userId);
        model.addAttribute("username", username);

        // ✅ 화면 표시용
        model.addAttribute("filterName", filterName);
        model.addAttribute("filterError", filterError);

        // ✅ 기간/페이징 유지
        model.addAttribute("start", from.toString());
        model.addAttribute("end", to.toString());
        model.addAttribute("page", p);
        model.addAttribute("size", s);

        Map<String,Object> params = new LinkedHashMap<>();
        params.put("userId", userId);
        params.put("username", username);
        params.put("start", from.toString());
        params.put("end", to.toString());
        params.put("size", s);
        model.addAttribute("params", params);

        return "admin/orders_list";
    }






    @GetMapping("/orders/{orderId}")
    public String adminOrderDetail(
            @PathVariable long orderId,
            HttpSession session,
            Model model
    ) throws Exception {
        if (!isAdmin(session)) return "redirect:/login";

        try (java.sql.Connection con = org.zerock.com.example.common.DBUtil.getConnection()) {
            // ✅ 관리자용: userId 체크 없이 orderId로 조회
            var header = orderDAO.findOrderHeaderAdmin(con, orderId);   // 아래 DAO 추가
            if (header == null) return "redirect:/admin/orders";

            var items = orderDAO.findOrderItems(con, orderId);

            model.addAttribute("header", header);
            model.addAttribute("items", items);
        }

        model.addAttribute("title", "주문 상세");
        return "admin/order_detail";
    }

    @PostMapping("/orders/{orderId}/cancel-confirm")
    public String adminCancelConfirm(
            @PathVariable long orderId,
            HttpSession session
    ) {
        if (!isAdmin(session)) return "redirect:/login";

        try {
            // ✅ 상태 검증/멱등성은 서비스 책임
            checkoutService.confirmCancelByAdmin(orderId);
            session.setAttribute("FLASH_MESSAGE", "취소가 확정되었습니다.");
        } catch (IllegalStateException e) {
            // 예: 이미 환불됨 / 취소요청 아님
            session.setAttribute("FLASH_MESSAGE", e.getMessage());
        } catch (Exception e) {
            session.setAttribute("FLASH_MESSAGE", "취소 처리 중 오류가 발생했습니다.");
        }

        return "redirect:/admin/orders/" + orderId;
    }


    @PostMapping("/members/{id}/points")
    public String adminAdjustPoints(
            @PathVariable long id,
            @RequestParam long delta,
            HttpSession session
    ) throws Exception {
        if (!isAdmin(session)) return "redirect:/login";

        // 최소 방어: 너무 큰 값 방지 (원하는 값으로 조정)
        long limit = 1_000_000_000L;
        if (delta > limit || delta < -limit) {
            session.setAttribute("FLASH_MESSAGE", "포인트 조정 값이 너무 큽니다.");
            return "redirect:/admin/members/" + id;
        }

        if (delta == 0) {
            session.setAttribute("FLASH_MESSAGE", "변경된 포인트가 없습니다.");
            return "redirect:/admin/members/" + id;
        }

        adminMemberService.adjustPoints(id, delta);

        session.setAttribute("FLASH_MESSAGE", "포인트가 조정되었습니다. (delta=" + delta + ")");
        return "redirect:/admin/members/" + id;
    }


    @PostMapping("/products/sale-bulk")
    public String adminProductsBulkSale(
            @RequestParam("ids") List<Long> ids,
            @RequestParam int percent,
            @RequestParam String saleStartAt,   // 예: 2026-01-21T12:30 (datetime-local)
            @RequestParam String saleEndAt,
            @RequestParam(required = false) String returnUrl,
            HttpSession session
    ) {
        if (!isAdmin(session)) return "redirect:/login";

        if (ids == null || ids.isEmpty()) {
            session.setAttribute("FLASH_MESSAGE", "선택된 상품이 없습니다.");
            return (returnUrl != null && !returnUrl.isBlank()) ? "redirect:" + returnUrl : "redirect:/admin/products";
        }

        // ✅ percent 방어 (1~99 권장)
        int p = Math.max(1, Math.min(99, percent));

        try {
            // ✅ input type="datetime-local" 형식 파싱
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            LocalDateTime start = LocalDateTime.parse(saleStartAt, fmt);
            LocalDateTime end   = LocalDateTime.parse(saleEndAt, fmt);

            if (!end.isAfter(start)) {
                session.setAttribute("FLASH_MESSAGE", "세일 종료일시는 시작일시보다 이후여야 합니다.");
                return (returnUrl != null && !returnUrl.isBlank()) ? "redirect:" + returnUrl : "redirect:/admin/products";
            }

            int updated = productDAO.applyPercentSale(ids, p, start, end);

            session.setAttribute("FLASH_MESSAGE",
                    "세일 적용 완료: " + updated + "개 상품 (" + p + "%, " + saleStartAt + " ~ " + saleEndAt + ")"
            );
        } catch (Exception e) {
            session.setAttribute("FLASH_MESSAGE", "세일 적용 실패: " + e.getMessage());
        }

        return (returnUrl != null && !returnUrl.isBlank()) ? "redirect:" + returnUrl : "redirect:/admin/products";
    }

    @PostMapping("/products/sale-bulk-cancel")
    public String adminProductsBulkSaleCancel(
            @RequestParam("ids") List<Long> ids,
            @RequestParam(required = false) String returnUrl,
            HttpSession session
    ) {
        if (!isAdmin(session)) return "redirect:/login";

        if (ids == null || ids.isEmpty()) {
            session.setAttribute("FLASH_MESSAGE", "선택된 상품이 없습니다.");
            return (returnUrl != null && !returnUrl.isBlank()) ? "redirect:" + returnUrl : "redirect:/admin/products";
        }

        try {
            int updated = productDAO.clearSalePeriod(ids); // on_sale=0 + 기간 null
            session.setAttribute("FLASH_MESSAGE", "세일 해제 완료: " + updated + "개 상품");
        } catch (Exception e) {
            session.setAttribute("FLASH_MESSAGE", "세일 해제 실패: " + e.getMessage());
        }

        return (returnUrl != null && !returnUrl.isBlank()) ? "redirect:" + returnUrl : "redirect:/admin/products";
    }
}
