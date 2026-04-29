package com.bank.dao;

import com.bank.model.Transaction;
import com.bank.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of {@link TransactionDAO}.
 */
public class TransactionDAOImpl implements TransactionDAO {

    private static final String SQL_INSERT =
        "INSERT INTO transactions " +
        "(reference_no, account_id, related_account, type, amount, balance_after, description) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_BY_ACCOUNT =
        "SELECT * FROM transactions WHERE account_id = ? ORDER BY created_at DESC LIMIT ?";

    @Override
    public Transaction create(Connection conn, Transaction t) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, t.getReferenceNo());
            ps.setLong  (2, t.getAccountId());
            if (t.getRelatedAccount() == null) {
                ps.setNull(3, Types.BIGINT);
            } else {
                ps.setLong(3, t.getRelatedAccount());
            }
            ps.setString    (4, t.getType().name());
            ps.setBigDecimal(5, t.getAmount());
            ps.setBigDecimal(6, t.getBalanceAfter());
            ps.setString    (7, t.getDescription());

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    t.setTransactionId(keys.getLong(1));
                }
            }
            return t;
        }
    }

    @Override
    public List<Transaction> findByAccount(long accountId, int limit) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_ACCOUNT)) {
            ps.setLong(1, accountId);
            ps.setInt (2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private Transaction map(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setTransactionId(rs.getLong("transaction_id"));
        t.setReferenceNo  (rs.getString("reference_no"));
        t.setAccountId    (rs.getLong("account_id"));
        long related = rs.getLong("related_account");
        t.setRelatedAccount(rs.wasNull() ? null : related);
        t.setType         (Transaction.Type.valueOf(rs.getString("type")));
        t.setAmount       (rs.getBigDecimal("amount"));
        t.setBalanceAfter (rs.getBigDecimal("balance_after"));
        t.setDescription  (rs.getString("description"));
        Timestamp c = rs.getTimestamp("created_at");
        if (c != null) t.setCreatedAt(c.toLocalDateTime());
        return t;
    }
}
