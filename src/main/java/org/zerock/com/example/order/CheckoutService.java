package org.zerock.com.example.order;

import org.springframework.stereotype.Service;
import org.zerock.com.example.cart.CartDAO;
import org.zerock.com.example.cart.CartItemDTO;
import org.zerock.com.example.common.DBUtil;
import org.zerock.com.example.pay.kakao.KakaoReadyResponse;
import org.zerock.com.example.pay.kakao.KakaoPayService;
import org.zerock.com.example.product.ProductDAO;
import org.zerock.com.example.user.*;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

@Service
public class CheckoutService {

    private final CartDAO cartDAO;
    private final OrderDAO orderDAO;
    private final UserDAO userDAO;
    private final GradePolicyDAO gradePolicyDAO;
    private final PointLedgerDAO pointLedgerDAO;
    private final KakaoPayService kakaoPayService;


    private final ProductDAO productDAO;   // ✅ 추가

    public CheckoutService(CartDAO cartDAO,
                           OrderDAO orderDAO,
                           UserDAO userDAO,
                           GradePolicyDAO gradePolicyDAO,
                           PointLedgerDAO pointLedgerDAO,
                           KakaoPayService kakaoPayService,
                           ProductDAO productDAO) { // ✅ 생성자 주입
        this.cartDAO = cartDAO;
        this.orderDAO = orderDAO;
        this.userDAO = userDAO;
        this.gradePolicyDAO = gradePolicyDAO;
        this.pointLedgerDAO = pointLedgerDAO;
        this.kakaoPayService = kakaoPayService;
        this.productDAO = productDAO;
    }

    // ✅ DB: 주문 draft 생성 + itemName/qty/total 계산
    public KakaoReadyContext createKakaoDraftAndBuildContext(long userId, String address) throws Exception {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                List<CartItemDTO> cart = cartDAO.list(con, userId);
                if (cart.isEmpty()) throw new IllegalStateException("Cart is empty");

                long total = 0L;
                int qty = 0;
                List<OrderItemDTO> items = new ArrayList<>();

                for (CartItemDTO c : cart) {
                    total += (long) c.getUnitPrice() * c.getQuantity();
                    qty += c.getQuantity();

                    OrderItemDTO it = new OrderItemDTO();
                    it.setProductId(c.getProductId());
                    it.setQuantity(c.getQuantity());
                    it.setUnitPrice(c.getUnitPrice());
                    items.add(it);
                }

                String itemName = cart.get(0).getProductName();
                if (cart.size() > 1) itemName = itemName + " 외 " + (cart.size() - 1) + "건";
                qty = Math.max(1, qty);

                String orderNo = "OD" + System.currentTimeMillis();
                long orderId = orderDAO.createOrder(con, userId, orderNo, total, address, "KAKAO", items);

                con.commit();
                return new KakaoReadyContext(orderId, orderNo, itemName, qty, total);

            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                try { con.setAutoCommit(true); } catch (Exception ignore) {}
            }
        }
    }

    // ✅ HTTP: Kakao ready 호출 → DB: tid 저장 & READY 전환
    public String startKakaoReady(long userId, String address) throws Exception {
        KakaoReadyContext ctx = createKakaoDraftAndBuildContext(userId, address);

        // 외부 HTTP 호출
        KakaoReadyResponse ready = kakaoPayService.ready(
                ctx.orderNo(),
                String.valueOf(userId),
                ctx.itemName(),
                ctx.quantity(),
                ctx.totalAmount()
        );

        if (ready == null || ready.getTid() == null || ready.getNextRedirectPcUrl() == null) {
            throw new IllegalStateException("kakao ready failed");
        }

        // tid 저장 + READY 전환
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                boolean ok = orderDAO.markReadyWithTidIfCreated(con, ctx.orderId(), ready.getTid());
                if (!ok) throw new IllegalStateException("Order not in CREATED state: " + ctx.orderId());
                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                try { con.setAutoCommit(true); } catch (Exception ignore) {}
            }
        }

        return ready.getNextRedirectPcUrl();
    }

    // ✅ approve 이후 정산/포인트/등급/카트비우기 로직은
    // 이미 “성공했던 시점” 코드가 있다면 그대로 유지하시면 됩니다.
        /* =========================
       approve 금액 검증
       ========================= */
    public void verifyAmount(long dbTotal, org.zerock.com.example.pay.kakao.KakaoApproveResponse approved) {
        Long paidTotal = (approved != null && approved.getAmount() != null)
                ? approved.getAmount().getTotal()
                : null;

        if (paidTotal == null) throw new IllegalStateException("Kakao approve missing amount.total");
        if (dbTotal != paidTotal.longValue()) {
            throw new IllegalStateException("Amount mismatch. db=" + dbTotal + ", kakao=" + paidTotal);
        }
    }

    /* =========================
       카카오 approve 성공 후 최종 확정 처리
       ========================= */
    public void finalizePaidAfterKakaoApprove(long userId, long orderId) throws Exception {
        try (java.sql.Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                boolean paid = orderDAO.markPaidIfReadyOrCreated(con, orderId);
                if (!paid) throw new IllegalStateException("Order not in READY/CREATED state: " + orderId);

                long total = orderDAO.getOrderTotalPrice(con, orderId);

                // 누적 구매금액 반영
                userDAO.addTotalSpent(con, userId, total);

                // 등급 재산정
                long newTotal = userDAO.getTotalSpent(con, userId);
                GradePolicyDTO policy = gradePolicyDAO.findPolicyByTotalSpent(con, newTotal);
                if (policy == null) throw new IllegalStateException("grade_policy not found for total=" + newTotal);

                userDAO.updateGrade(con, userId, policy.getGrade());

                // 주문 snapshot(구매 시점 등급/적립/할인율) 저장
                orderDAO.updateSnapshotRates(con, orderId,
                        policy.getGrade(),
                        policy.getDiscountRate() == null ? java.math.BigDecimal.ZERO : policy.getDiscountRate(),
                        policy.getPointRate() == null ? java.math.BigDecimal.ZERO : policy.getPointRate());

                // 포인트 적립
                java.math.BigDecimal pr = policy.getPointRate() == null ? java.math.BigDecimal.ZERO : policy.getPointRate();
                long earnedPoint = (long) Math.floor(total * pr.doubleValue() / 100.0);
                if (earnedPoint > 0) {
                    userDAO.addPointBalance(con, userId, earnedPoint);
                    pointLedgerDAO.insert(con, userId, orderId, earnedPoint, "ORDER_EARN");
                }

                // 결제 성공 시 장바구니 비우기
                cartDAO.clear(con, userId);

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                try { con.setAutoCommit(true); } catch (Exception ignore) {}
            }
        }
    }

    public UserDTO reloadUser(long userId) throws Exception {
        try (java.sql.Connection con = DBUtil.getConnection()) {
            return userDAO.findById(con, userId);
        }
    }
    public OrderResult checkoutAllInOne(long userId, String address, String payMethod) throws Exception {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                List<CartItemDTO> cart = cartDAO.list(con, userId);
                if (cart.isEmpty()) throw new IllegalStateException("Cart is empty");

                long total = 0L;
                List<OrderItemDTO> items = new ArrayList<>();

                for (CartItemDTO c : cart) {
                    total += (long) c.getUnitPrice() * c.getQuantity();

                    OrderItemDTO it = new OrderItemDTO();
                    it.setProductId(c.getProductId());
                    it.setQuantity(c.getQuantity());
                    it.setUnitPrice(c.getUnitPrice());
                    items.add(it);
                }

                String orderNo = "OD" + System.currentTimeMillis();
                long orderId = orderDAO.createOrder(con, userId, orderNo, total, address, payMethod, items);

                boolean paid = orderDAO.markPaidIfCreated(con, orderId);
                if (!paid) throw new IllegalStateException("Order not in CREATED: " + orderId);

                // 공통 정산(등급/포인트/카트비우기)까지 하고 싶으면 여기서 호출
                // finalizePaidCommon(con, userId, orderId);

                con.commit();
                return new OrderResult(orderId, orderNo, total);
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                try { con.setAutoCommit(true); } catch (Exception ignore) {}
            }
        }
    }
    public void confirmPurchase(long userId, long orderId) throws Exception {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                // 1️⃣ 주문 상태 PAID → CONFIRMED
                boolean ok = orderDAO.markConfirmedIfPaid(con, orderId, userId);
                if (!ok) {
                    throw new IllegalStateException(
                            "Order not in PAID or not yours: " + orderId
                    );
                }

                // 2️⃣ 주문 아이템 목록 조회
                List<OrderItemDTO> items =
                        orderDAO.listOrderItems(con, orderId);

                // 3️⃣ 상품별 purchase_count 증가
                for (OrderItemDTO it : items) {
                    productDAO.increasePurchaseCount(
                            con,
                            it.getProductId(),
                            it.getQuantity()
                    );
                }

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public void requestCancel(long userId, long orderId, String reason) throws Exception {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                boolean ok = orderDAO.markCancelRequestedIfPaid(con, orderId, userId, reason);
                if (!ok) throw new IllegalStateException("Order not in PAID or not yours: " + orderId);
                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }
    public void confirmCancelByAdmin(long orderId) throws Exception {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                // 1) 상태를 REFUNDED로 전환 (CANCEL_REQUESTED에서만 1회 성공)
                boolean ok = orderDAO.markRefundedIfCancelRequested(con, orderId);
                if (!ok) {
                    throw new IllegalStateException("취소요청 상태가 아닙니다. orderId=" + orderId);
                }

                // 2) 주문/회원 정보
                OrderDAO.RefundBase base = orderDAO.findRefundBase(con, orderId);
                if (base == null) throw new IllegalStateException("order not found: " + orderId);

                long userId = base.userId();
                long totalPrice = base.totalPrice();

                // 3) 이 주문으로 적립된 포인트 합
                long earned = pointLedgerDAO.sumEarnedPointsByOrder(con, orderId);

                // ✅ 중복 회수 방지: 이미 ORDER_REVOKE가 있으면 earned=0 처리
                if (earned > 0 && pointLedgerDAO.existsRevokeByOrder(con, orderId)) {
                    earned = 0;
                }

                // 4) 포인트 회수 + ledger 기록
                if (earned > 0) {
                    userDAO.addPointBalance(con, userId, -earned);
                    pointLedgerDAO.insert(con, userId, orderId, -earned, "ORDER_REVOKE");
                }

                // 5) 누적 구매금액 차감
                userDAO.addTotalSpent(con, userId, -totalPrice);

                // 6) 등급 재산정 (✅ users.total_spent 기준)
                long newTotal = userDAO.getTotalSpent(con, userId);
                GradePolicyDTO policy = gradePolicyDAO.findPolicyByTotalSpent(con, newTotal);
                if (policy == null) throw new IllegalStateException("grade_policy not found for total=" + newTotal);

                userDAO.updateGrade(con, userId, policy.getGrade());

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }






}




