-- =====================================================
-- Bank Management System - Database Schema
-- Database: MySQL 8.0+
-- =====================================================

CREATE DATABASE IF NOT EXISTS bank_db;
USE bank_db;

-- ----------------------------
-- Accounts Table
-- ----------------------------
CREATE TABLE IF NOT EXISTS accounts (
    account_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number  VARCHAR(20) NOT NULL UNIQUE,
    holder_name     VARCHAR(100) NOT NULL,
    email           VARCHAR(100) NOT NULL,
    phone           VARCHAR(20)  NOT NULL,
    account_type    ENUM('SAVINGS', 'CURRENT') NOT NULL DEFAULT 'SAVINGS',
    balance         DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    pin_hash        VARCHAR(255) NOT NULL,
    status          ENUM('ACTIVE', 'CLOSED', 'FROZEN') NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_account_number (account_number),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Transactions Table
-- ----------------------------
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_no     VARCHAR(40) NOT NULL UNIQUE,
    account_id       BIGINT NOT NULL,
    related_account  BIGINT NULL,
    type             ENUM('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT') NOT NULL,
    amount           DECIMAL(15, 2) NOT NULL,
    balance_after    DECIMAL(15, 2) NOT NULL,
    description      VARCHAR(255),
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id)      REFERENCES accounts(account_id) ON DELETE CASCADE,
    FOREIGN KEY (related_account) REFERENCES accounts(account_id) ON DELETE SET NULL,
    INDEX idx_account_id (account_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
