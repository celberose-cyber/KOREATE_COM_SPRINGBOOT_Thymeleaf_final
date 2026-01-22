package org.zerock.com.example.admin;

import org.springframework.stereotype.Service;
import org.zerock.com.example.common.DBUtil;
import org.zerock.com.example.user.PointLedgerDAO;
import org.zerock.com.example.user.UserDAO;

import java.sql.Connection;

@Service
public class AdminMemberService {

    private final UserDAO userDAO;
    private final PointLedgerDAO pointLedgerDAO;

    public AdminMemberService(UserDAO userDAO, PointLedgerDAO pointLedgerDAO) {
        this.userDAO = userDAO;
        this.pointLedgerDAO = pointLedgerDAO;
    }

    /**
     * 관리자 포인트 조정 (증가/차감)
     * - users.point_balance 변경
     * - point_ledger 기록 (order_id = null, reason = ADMIN_ADJUST)
     */
    public void adjustPoints(long userId, long delta) throws Exception {
        if (delta == 0) return;

        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                // (선택) 차감인데 잔액 부족하면 실패시키기
                if (delta < 0) {
                    long bal = userDAO.getPointBalance(con, userId);
                    if (bal + delta < 0) { // delta는 음수
                        throw new IllegalStateException("포인트 잔액 부족 (balance=" + bal + ", delta=" + delta + ")");
                    }
                }

                int updated = userDAO.addPointBalance(con, userId, delta);
                if (updated != 1) throw new IllegalStateException("user not found: " + userId);

                pointLedgerDAO.insert(con, userId, null, delta, "ADMIN_ADJUST");

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
