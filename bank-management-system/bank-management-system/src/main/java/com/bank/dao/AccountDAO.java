package com.bank.dao;

import com.bank.model.Account;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object contract for {@link Account} persistence.
 */
public interface AccountDAO {

    /** Insert a new account and populate its generated id. */
    Account create(Account account) throws SQLException;

    /** Find an account by its primary key. */
    Optional<Account> findById(long accountId) throws SQLException;

    /** Find an account by its public account number. */
    Optional<Account> findByAccountNumber(String accountNumber) throws SQLException;

    /** List all accounts (sorted by created_at DESC). */
    List<Account> findAll() throws SQLException;

    /** Update mutable fields (holder info, status). */
    boolean update(Account account) throws SQLException;

    /** Soft-delete by setting status = CLOSED. */
    boolean closeAccount(long accountId) throws SQLException;

    /**
     * Atomically adjust an account's balance using the supplied connection
     * (so it can participate in a transaction). {@code delta} may be negative.
     * Returns the new balance, or empty if the row was not found / inactive.
     */
    Optional<BigDecimal> adjustBalance(Connection conn, long accountId, BigDecimal delta)
            throws SQLException;
}
