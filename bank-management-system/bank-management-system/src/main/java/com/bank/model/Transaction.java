package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model representing a transaction recorded against an account.
 */
public class Transaction {

    public enum Type { DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT }

    private long          transactionId;
    private String        referenceNo;
    private long          accountId;
    private Long          relatedAccount;   // nullable
    private Type          type;
    private BigDecimal    amount;
    private BigDecimal    balanceAfter;
    private String        description;
    private LocalDateTime createdAt;

    public Transaction() {}

    public Transaction(String referenceNo, long accountId, Long relatedAccount,
                       Type type, BigDecimal amount, BigDecimal balanceAfter,
                       String description) {
        this.referenceNo     = referenceNo;
        this.accountId       = accountId;
        this.relatedAccount  = relatedAccount;
        this.type            = type;
        this.amount          = amount;
        this.balanceAfter    = balanceAfter;
        this.description     = description;
    }

    // ---------------- Getters & Setters ----------------

    public long getTransactionId()              { return transactionId; }
    public void setTransactionId(long id)       { this.transactionId = id; }

    public String getReferenceNo()              { return referenceNo; }
    public void setReferenceNo(String r)        { this.referenceNo = r; }

    public long getAccountId()                  { return accountId; }
    public void setAccountId(long id)           { this.accountId = id; }

    public Long getRelatedAccount()             { return relatedAccount; }
    public void setRelatedAccount(Long r)       { this.relatedAccount = r; }

    public Type getType()                       { return type; }
    public void setType(Type t)                 { this.type = t; }

    public BigDecimal getAmount()               { return amount; }
    public void setAmount(BigDecimal a)         { this.amount = a; }

    public BigDecimal getBalanceAfter()         { return balanceAfter; }
    public void setBalanceAfter(BigDecimal b)   { this.balanceAfter = b; }

    public String getDescription()              { return description; }
    public void setDescription(String d)        { this.description = d; }

    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime c)   { this.createdAt = c; }

    @Override
    public String toString() {
        return String.format(
            "[%s] %-13s %12s  bal=%s  ref=%s  %s",
            createdAt, type, amount, balanceAfter, referenceNo,
            description == null ? "" : description);
    }
}
