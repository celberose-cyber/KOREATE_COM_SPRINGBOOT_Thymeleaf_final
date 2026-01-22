package org.zerock.com.example.cart;

import org.springframework.stereotype.Service;
import org.zerock.com.example.common.DBUtil;
import org.zerock.com.example.user.GradePolicyDAO;
import org.zerock.com.example.user.GradePolicyDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.List;

@Service
public class CartService {

    private final GradePolicyDAO gradePolicyDAO;

    public CartService(GradePolicyDAO gradePolicyDAO) {
        this.gradePolicyDAO = gradePolicyDAO;
    }

    public CartSummaryDTO summarize(List<CartItemDTO> items, long userTotalSpent) throws Exception {
        long total = items.stream()
                .mapToLong(i -> i.getUnitPrice() * (long) i.getQuantity())
                .sum();

        try (Connection con = DBUtil.getConnection()) {

            GradePolicyDTO policy = gradePolicyDAO.findPolicyByTotalSpent(con, userTotalSpent);

            BigDecimal discountRate = (policy == null || policy.getDiscountRate() == null)
                    ? BigDecimal.ZERO : policy.getDiscountRate();

            BigDecimal pointRate = (policy == null || policy.getPointRate() == null)
                    ? BigDecimal.ZERO : policy.getPointRate();

            BigDecimal totalBD = BigDecimal.valueOf(total);

            BigDecimal discountAmountBD = totalBD
                    .multiply(discountRate)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);

            BigDecimal discountedTotalBD = totalBD.subtract(discountAmountBD);

            BigDecimal expectedPointBD = discountedTotalBD
                    .multiply(pointRate)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);

            return new CartSummaryDTO(
                    total,
                    discountAmountBD.longValue(),
                    discountedTotalBD.longValue(),
                    expectedPointBD.longValue(),
                    policy
            );
        }
    }
}
