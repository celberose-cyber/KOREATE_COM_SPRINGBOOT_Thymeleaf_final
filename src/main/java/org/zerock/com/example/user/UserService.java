package org.zerock.com.example.user;

import org.springframework.stereotype.Service;
import org.zerock.com.example.common.DBUtil;
import org.zerock.com.example.order.OrderDAO;

import java.sql.Connection;
@Service
public class UserService {

    private final UserDAO userDAO;
    private final OrderDAO orderDAO;
    private final GradePolicyDAO gradePolicyDAO;
    private final PointLedgerDAO pointLedgerDAO;

    public UserService(
            UserDAO userDAO,
            OrderDAO orderDAO,
            GradePolicyDAO gradePolicyDAO,
            PointLedgerDAO pointLedgerDAO
    ) {
        this.userDAO = userDAO;
        this.orderDAO = orderDAO;
        this.gradePolicyDAO = gradePolicyDAO;
        this.pointLedgerDAO = pointLedgerDAO;
    }

    public void payComplete(long userId, long orderId) throws Exception {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                boolean firstPaid = orderDAO.markPaidIfCreated(con, orderId);
                if (!firstPaid) {
                    con.rollback();
                    return;
                }

                long paidAmount = orderDAO.getOrderTotalPrice(con, orderId);

                userDAO.addTotalSpent(con, userId, paidAmount);

                long total = orderDAO.totalSpentByUser(con, userId);
                GradePolicyDTO policy = gradePolicyDAO.findPolicyByTotalSpent(con, total);
                if (policy == null) throw new IllegalStateException("grade_policy not found for total=" + total);

                userDAO.updateGrade(con, userId, policy.getGrade());

                orderDAO.updateSnapshotRates(con, orderId,
                        policy.getGrade(), policy.getDiscountRate(), policy.getPointRate());

                long earnedPoint = (long) Math.floor(paidAmount * policy.getPointRate().doubleValue() / 100.0);
                if (earnedPoint > 0) {
                    userDAO.addPointBalance(con, userId, earnedPoint);
                    pointLedgerDAO.insert(con, userId, orderId, earnedPoint, "ORDER_EARN");
                }

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                try { con.setAutoCommit(true); } catch (Exception ignore) {}
            }
        }
    }
}


