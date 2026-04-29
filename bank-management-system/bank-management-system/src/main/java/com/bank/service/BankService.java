package com.bank.service;

import com.bank.dao.AccountDAO;
import com.bank.dao.AccountDAOImpl;
import com.bank.dao.TransactionDAO;
import com.bank.dao.TransactionDAOImpl;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.util.DBConnection;
import com.bank.util.PasswordUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service layer — encapsulates business rules and ensures atomic
 * (transactional) money-movement operations.
 */
public class BankService {

    private final AccountDAO     accountDAO;
    private final TransactionDAO transactionDAO;

    public BankService() {
        this(new AccountDAOImpl(), new TransactionDAOImpl());
    }

    public BankService(AccountDAO accountDAO, TransactionDAO transactionDAO) {
        this.accountDAO     = accountDAO;
        this.transactionDAO = transactionDAO;
    }

    // ============================================================
    //  Account management
    // ============================================================

    public Account openAccount(String holderName, String email, String phone,
                               Account.Type type, BigDecimal initialDeposit,
                               String pin) {

        if (holderName == null || holderName.isBlank())
            throw new BankServiceException("Holder name is required.");
        if (initialDeposit == null || initialDeposit.signum() < 0)
            throw new BankServiceException("Initial deposit cannot be negative.");
        if (pin == null || pin.length() < 4)
            throw new BankServiceException("PIN must be at least 4 digits.");

        Account a = new Account(
            generateAccountNumber(),
            holderName.trim(),
            email == null ? "" : email.trim(),
            phone == null ? "" : phone.trim(),
            type == null ? Account.Type.SAVINGS : type,
            initialDeposit,
            PasswordUtil.hash(pin)
        );

        try {
            accountDAO.create(a);

            // Record the opening deposit as a transaction (if any).
            if (initialDeposit.signum() > 0) {
                try (Connection conn = DBConnection.getConnection()) {
                    Transaction opening = new Transaction(
                        newReference(), a.getAccountId(), null,
                        Transaction.Type.DEPOSIT, initialDeposit,
                        initialDeposit, "Opening deposit"
                    );
                    transactionDAO.create(conn, opening);
                }
            }
            return a;
        } catch (SQLException e) {
            throw new BankServiceException("Failed to open account: " + e.getMessage(), e);
        }
    }

    public Optional<Account> findByNumber(String accountNumber) {
        try {
            return accountDAO.findByAccountNumber(accountNumber);
        } catch (SQLException e) {
            throw new BankServiceException("Lookup failed: " + e.getMessage(), e);
        }
    }

    public List<Account> listAccounts() {
        try {
            return accountDAO.findAll();
        } catch (SQLException e) {
            throw new BankServiceException("List failed: " + e.getMessage(), e);
        }
    }

    public boolean closeAccount(String accountNumber, String pin) {
        Account a = requireAccount(accountNumber);
        verifyPin(a, pin);
        if (a.getBalance().signum() != 0) {
            throw new BankServiceException(
                "Cannot close account with non-zero balance. Withdraw funds first.");
        }
        try {
            return accountDAO.closeAccount(a.getAccountId());
        } catch (SQLException e) {
            throw new BankServiceException("Close failed: " + e.getMessage(), e);
        }
    }

    // ============================================================
    //  Money movement (atomic)
    // ============================================================

    public Transaction deposit(String accountNumber, BigDecimal amount, String description) {
        validatePositive(amount);
        Account a = requireAccount(accountNumber);

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal newBalance = accountDAO
                    .adjustBalance(conn, a.getAccountId(), amount)
                    .orElseThrow(() -> new BankServiceException("Account not found."));

                Transaction t = transactionDAO.create(conn, new Transaction(
                    newReference(), a.getAccountId(), null,
                    Transaction.Type.DEPOSIT, amount, newBalance,
                    description == null ? "Cash deposit" : description));

                conn.commit();
                return t;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new BankServiceException("Deposit failed: " + e.getMessage(), e);
        }
    }

    public Transaction withdraw(String accountNumber, BigDecimal amount, String pin,
                                String description) {
        validatePositive(amount);
        Account a = requireAccount(accountNumber);
        verifyPin(a, pin);

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal newBalance = accountDAO
                    .adjustBalance(conn, a.getAccountId(), amount.negate())
                    .orElseThrow(() -> new BankServiceException("Account not found."));

                Transaction t = transactionDAO.create(conn, new Transaction(
                    newReference(), a.getAccountId(), null,
                    Transaction.Type.WITHDRAWAL, amount, newBalance,
                    description == null ? "Cash withdrawal" : description));

                conn.commit();
                return t;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new BankServiceException("Withdrawal failed: " + e.getMessage(), e);
        }
    }

    /**
     * Transfer funds between two accounts atomically. Both legs (debit + credit)
     * succeed or both roll back.
     */
    public void transfer(String fromAccountNo, String toAccountNo,
                         BigDecimal amount, String pin, String description) {

        validatePositive(amount);
        if (fromAccountNo.equalsIgnoreCase(toAccountNo)) {
            throw new BankServiceException("Cannot transfer to the same account.");
        }
        Account from = requireAccount(fromAccountNo);
        Account to   = requireAccount(toAccountNo);
        verifyPin(from, pin);

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Lock rows in a deterministic order to avoid deadlocks.
                long firstId  = Math.min(from.getAccountId(), to.getAccountId());
                long secondId = Math.max(from.getAccountId(), to.getAccountId());

                BigDecimal fromBal, toBal;
                if (firstId == from.getAccountId()) {
                    fromBal = accountDAO.adjustBalance(conn, from.getAccountId(), amount.negate())
                        .orElseThrow(() -> new BankServiceException("Source account vanished."));
                    toBal   = accountDAO.adjustBalance(conn, to.getAccountId(),   amount)
                        .orElseThrow(() -> new BankServiceException("Target account vanished."));
                } else {
                    toBal   = accountDAO.adjustBalance(conn, to.getAccountId(),   amount)
                        .orElseThrow(() -> new BankServiceException("Target account vanished."));
                    fromBal = accountDAO.adjustBalance(conn, from.getAccountId(), amount.negate())
                        .orElseThrow(() -> new BankServiceException("Source account vanished."));
                }
                // Unused vars guard against accidental removal.
                if (firstId == secondId) { /* unreachable */ }

                String ref  = newReference();
                String desc = description == null
                    ? "Transfer " + from.getAccountNumber() + " -> " + to.getAccountNumber()
                    : description;

                transactionDAO.create(conn, new Transaction(
                    ref, from.getAccountId(), to.getAccountId(),
                    Transaction.Type.TRANSFER_OUT, amount, fromBal, desc));

                transactionDAO.create(conn, new Transaction(
                    ref, to.getAccountId(), from.getAccountId(),
                    Transaction.Type.TRANSFER_IN, amount, toBal, desc));

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new BankServiceException("Transfer failed: " + e.getMessage(), e);
        }
    }

    // ============================================================
    //  Inquiries
    // ============================================================

    public BigDecimal getBalance(String accountNumber, String pin) {
        Account a = requireAccount(accountNumber);
        verifyPin(a, pin);
        return a.getBalance();
    }

    public List<Transaction> getStatement(String accountNumber, String pin, int limit) {
        Account a = requireAccount(accountNumber);
        verifyPin(a, pin);
        try {
            return transactionDAO.findByAccount(a.getAccountId(), limit);
        } catch (SQLException e) {
            throw new BankServiceException("Statement failed: " + e.getMessage(), e);
        }
    }

    // ============================================================
    //  Helpers
    // ============================================================

    private Account requireAccount(String accountNumber) {
        return findByNumber(accountNumber)
            .orElseThrow(() -> new BankServiceException(
                "Account not found: " + accountNumber));
    }

    private void verifyPin(Account a, String pin) {
        if (pin == null || !PasswordUtil.matches(pin, a.getPinHash())) {
            throw new BankServiceException("Invalid PIN.");
        }
        if (a.getStatus() != Account.Status.ACTIVE) {
            throw new BankServiceException("Account is " + a.getStatus());
        }
    }

    private void validatePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BankServiceException("Amount must be positive.");
        }
    }

    private String generateAccountNumber() {
        // 12-digit account number, prefixed with bank code 1001.
        long suffix = ThreadLocalRandom.current().nextLong(10_000_000L, 100_000_000L);
        return "1001" + suffix;
    }

    private String newReference() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }
}
