package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model representing a bank account.
 */
public class Account {

    public enum Type   { SAVINGS, CURRENT }
    public enum Status { ACTIVE, CLOSED, FROZEN }

    private long          accountId;
    private String        accountNumber;
    private String        holderName;
    private String        email;
    private String        phone;
    private Type          accountType;
    private BigDecimal    balance;
    private String        pinHash;
    private Status        status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Account() {}

    public Account(String accountNumber, String holderName, String email,
                   String phone, Type accountType, BigDecimal balance,
                   String pinHash) {
        this.accountNumber = accountNumber;
        this.holderName    = holderName;
        this.email         = email;
        this.phone         = phone;
        this.accountType   = accountType;
        this.balance       = balance;
        this.pinHash       = pinHash;
        this.status        = Status.ACTIVE;
    }

    // ---------------- Getters & Setters ----------------

    public long getAccountId()              { return accountId; }
    public void setAccountId(long id)       { this.accountId = id; }

    public String getAccountNumber()        { return accountNumber; }
    public void setAccountNumber(String n)  { this.accountNumber = n; }

    public String getHolderName()           { return holderName; }
    public void setHolderName(String n)     { this.holderName = n; }

    public String getEmail()                { return email; }
    public void setEmail(String e)          { this.email = e; }

    public String getPhone()                { return phone; }
    public void setPhone(String p)          { this.phone = p; }

    public Type getAccountType()            { return accountType; }
    public void setAccountType(Type t)      { this.accountType = t; }

    public BigDecimal getBalance()          { return balance; }
    public void setBalance(BigDecimal b)    { this.balance = b; }

    public String getPinHash()              { return pinHash; }
    public void setPinHash(String h)        { this.pinHash = h; }

    public Status getStatus()               { return status; }
    public void setStatus(Status s)         { this.status = s; }

    public LocalDateTime getCreatedAt()     { return createdAt; }
    public void setCreatedAt(LocalDateTime c){ this.createdAt = c; }

    public LocalDateTime getUpdatedAt()     { return updatedAt; }
    public void setUpdatedAt(LocalDateTime u){ this.updatedAt = u; }

    @Override
    public String toString() {
        return String.format(
            "Account[no=%s, holder=%s, type=%s, balance=%s, status=%s]",
            accountNumber, holderName, accountType, balance, status);
    }
}
