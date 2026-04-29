package com.bank.dao;

import com.bank.model.Transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object contract for {@link Transaction} persistence.
 */
public interface TransactionDAO {

    /**
     * Insert a transaction using the supplied connection (so it can be
     * part of an atomic deposit/withdrawal/transfer operation).
     */
    Transaction create(Connection conn, Transaction txn) throws SQLException;

    /** Return the most recent transactions for an account. */
    List<Transaction> findByAccount(long accountId, int limit) throws SQLException;
}
