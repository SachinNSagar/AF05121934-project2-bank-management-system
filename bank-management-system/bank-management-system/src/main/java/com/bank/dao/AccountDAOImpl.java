package com.bank.dao;

import com.bank.model.Account;
import com.bank.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link AccountDAO}.
 */
public class AccountDAOImpl implements AccountDAO {

    private static final String SQL_INSERT =
        "INSERT INTO accounts " +
        "(account_number, holder_name, email, phone, account_type, balance, pin_hash, status) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_BY_ID =
        "SELECT * FROM accounts WHERE account_id = ?";

    private static final String SQL_SELECT_BY_NUMBER =
        "SELECT * FROM accounts WHERE account_number = ?";

    private static final String SQL_SELECT_ALL =
        "SELECT * FROM accounts ORDER BY created_at DESC";

    private static final String SQL_UPDATE =
        "UPDATE accounts SET holder_name = ?, email = ?, phone = ?, status = ? " +
        "WHERE account_id = ?";

    private static final String SQL_CLOSE =
        "UPDATE accounts SET status = 'CLOSED' WHERE account_id = ?";

    // Locked update — relies on row lock from SELECT ... FOR UPDATE in caller transaction.
    private static final String SQL_LOCK_ROW =
        "SELECT balance, status FROM accounts WHERE account_id = ? FOR UPDATE";

    private static final String SQL_UPDATE_BALANCE =
        "UPDATE accounts SET balance = ? WHERE account_id = ?";

    @Override
    public Account create(Account a) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, a.getAccountNumber());
            ps.setString(2, a.getHolderName());
            ps.setString(3, a.getEmail());
            ps.setString(4, a.getPhone());
            ps.setString(5, a.getAccountType().name());
            ps.setBigDecimal(6, a.getBalance());
            ps.setString(7, a.getPinHash());
            ps.setString(8, a.getStatus().name());

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    a.setAccountId(keys.getLong(1));
                }
            }
            return a;
        }
    }

    @Override
    public Optional<Account> findById(long accountId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_ID)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_NUMBER)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Account> findAll() throws SQLException {
        List<Account> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    @Override
    public boolean update(Account a) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, a.getHolderName());
            ps.setString(2, a.getEmail());
            ps.setString(3, a.getPhone());
            ps.setString(4, a.getStatus().name());
            ps.setLong(5, a.getAccountId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean closeAccount(long accountId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_CLOSE)) {
            ps.setLong(1, accountId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public Optional<BigDecimal> adjustBalance(Connection conn, long accountId, BigDecimal delta)
            throws SQLException {

        // 1. Lock the row & read current balance + status.
        BigDecimal current;
        String status;
        try (PreparedStatement lock = conn.prepareStatement(SQL_LOCK_ROW)) {
            lock.setLong(1, accountId);
            try (ResultSet rs = lock.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                current = rs.getBigDecimal("balance");
                status  = rs.getString("status");
            }
        }

        if (!"ACTIVE".equals(status)) {
            throw new SQLException("Account " + accountId + " is not ACTIVE (status=" + status + ")");
        }

        BigDecimal updated = current.add(delta);
        if (updated.signum() < 0) {
            throw new SQLException("Insufficient funds. Current=" + current + ", delta=" + delta);
        }

        // 2. Persist the new balance.
        try (PreparedStatement upd = conn.prepareStatement(SQL_UPDATE_BALANCE)) {
            upd.setBigDecimal(1, updated);
            upd.setLong(2, accountId);
            upd.executeUpdate();
        }
        return Optional.of(updated);
    }

    // ---------------- Row mapper ----------------

    private Account map(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setAccountId(rs.getLong("account_id"));
        a.setAccountNumber(rs.getString("account_number"));
        a.setHolderName(rs.getString("holder_name"));
        a.setEmail(rs.getString("email"));
        a.setPhone(rs.getString("phone"));
        a.setAccountType(Account.Type.valueOf(rs.getString("account_type")));
        a.setBalance(rs.getBigDecimal("balance"));
        a.setPinHash(rs.getString("pin_hash"));
        a.setStatus(Account.Status.valueOf(rs.getString("status")));
        Timestamp c = rs.getTimestamp("created_at");
        Timestamp u = rs.getTimestamp("updated_at");
        if (c != null) a.setCreatedAt(c.toLocalDateTime());
        if (u != null) a.setUpdatedAt(u.toLocalDateTime());
        return a;
    }
}
