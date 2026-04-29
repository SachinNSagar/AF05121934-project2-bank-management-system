package com.bank.service;

/**
 * Thrown by the service layer to wrap business / persistence failures.
 */
public class BankServiceException extends RuntimeException {
    public BankServiceException(String message)            { super(message); }
    public BankServiceException(String message, Throwable c){ super(message, c); }
}
