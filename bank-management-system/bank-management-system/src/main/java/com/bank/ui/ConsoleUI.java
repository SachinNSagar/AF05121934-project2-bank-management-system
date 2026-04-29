package com.bank.ui;

import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.service.BankService;
import com.bank.service.BankServiceException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

/**
 * Console-based user interface for the Bank Management System.
 */
public class ConsoleUI {

    private final Scanner     in      = new Scanner(System.in);
    private final BankService service = new BankService();

    public void start() {
        printBanner();
        boolean running = true;
        while (running) {
            printMenu();
            String choice = in.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> openAccount();
                    case "2" -> deposit();
                    case "3" -> withdraw();
                    case "4" -> transfer();
                    case "5" -> checkBalance();
                    case "6" -> miniStatement();
                    case "7" -> listAccounts();
                    case "8" -> closeAccount();
                    case "0" -> { running = false; System.out.println("Goodbye!"); }
                    default  -> System.out.println("Invalid option. Try again.");
                }
            } catch (BankServiceException ex) {
                System.out.println("ERROR: " + ex.getMessage());
            } catch (Exception ex) {
                System.out.println("Unexpected error: " + ex.getMessage());
            }
        }
    }

    // ---------------- Menu ----------------

    private void printBanner() {
        System.out.println("=========================================");
        System.out.println("   BANK MANAGEMENT SYSTEM (JDBC + MySQL) ");
        System.out.println("=========================================");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("------------- MAIN MENU -----------------");
        System.out.println(" 1. Open new account");
        System.out.println(" 2. Deposit");
        System.out.println(" 3. Withdraw");
        System.out.println(" 4. Transfer funds");
        System.out.println(" 5. Check balance");
        System.out.println(" 6. Mini statement (last 10 txns)");
        System.out.println(" 7. List all accounts");
        System.out.println(" 8. Close account");
        System.out.println(" 0. Exit");
        System.out.print("Choose an option: ");
    }

    // ---------------- Actions ----------------

    private void openAccount() {
        System.out.println("\n-- Open New Account --");
        String name  = prompt("Holder name      : ");
        String email = prompt("Email            : ");
        String phone = prompt("Phone            : ");
        Account.Type type = "C".equalsIgnoreCase(
            prompt("Type [S]avings/[C]urrent: ")) ? Account.Type.CURRENT : Account.Type.SAVINGS;
        BigDecimal initial = new BigDecimal(prompt("Initial deposit  : "));
        String pin = prompt("Set 4-digit PIN  : ");

        Account a = service.openAccount(name, email, phone, type, initial, pin);
        System.out.println("\nAccount opened successfully!");
        System.out.println("  Account Number : " + a.getAccountNumber());
        System.out.println("  Holder         : " + a.getHolderName());
        System.out.println("  Type           : " + a.getAccountType());
        System.out.println("  Balance        : " + a.getBalance());
    }

    private void deposit() {
        System.out.println("\n-- Deposit --");
        String acc = prompt("Account number : ");
        BigDecimal amt = new BigDecimal(prompt("Amount         : "));
        String desc = prompt("Description (opt): ");

        Transaction t = service.deposit(acc, amt, desc.isBlank() ? null : desc);
        System.out.println("Deposited. New balance = " + t.getBalanceAfter()
            + " (ref " + t.getReferenceNo() + ")");
    }

    private void withdraw() {
        System.out.println("\n-- Withdraw --");
        String acc = prompt("Account number : ");
        BigDecimal amt = new BigDecimal(prompt("Amount         : "));
        String pin = prompt("PIN            : ");
        String desc = prompt("Description (opt): ");

        Transaction t = service.withdraw(acc, amt, pin, desc.isBlank() ? null : desc);
        System.out.println("Withdrawn. New balance = " + t.getBalanceAfter()
            + " (ref " + t.getReferenceNo() + ")");
    }

    private void transfer() {
        System.out.println("\n-- Transfer Funds --");
        String from = prompt("From account   : ");
        String to   = prompt("To   account   : ");
        BigDecimal amt = new BigDecimal(prompt("Amount         : "));
        String pin  = prompt("Sender PIN     : ");
        String desc = prompt("Description (opt): ");

        service.transfer(from, to, amt, pin, desc.isBlank() ? null : desc);
        System.out.println("Transfer of " + amt + " from " + from + " -> " + to + " completed.");
    }

    private void checkBalance() {
        System.out.println("\n-- Check Balance --");
        String acc = prompt("Account number : ");
        String pin = prompt("PIN            : ");
        BigDecimal bal = service.getBalance(acc, pin);
        System.out.println("Available balance: " + bal);
    }

    private void miniStatement() {
        System.out.println("\n-- Mini Statement --");
        String acc = prompt("Account number : ");
        String pin = prompt("PIN            : ");
        List<Transaction> txns = service.getStatement(acc, pin, 10);
        if (txns.isEmpty()) {
            System.out.println("No transactions yet.");
            return;
        }
        System.out.println();
        txns.forEach(System.out::println);
    }

    private void listAccounts() {
        System.out.println("\n-- All Accounts --");
        List<Account> accounts = service.listAccounts();
        if (accounts.isEmpty()) {
            System.out.println("No accounts yet.");
            return;
        }
        System.out.printf("%-15s  %-25s  %-10s  %15s  %-8s%n",
            "ACCOUNT NO", "HOLDER", "TYPE", "BALANCE", "STATUS");
        System.out.println("-".repeat(80));
        for (Account a : accounts) {
            System.out.printf("%-15s  %-25s  %-10s  %15s  %-8s%n",
                a.getAccountNumber(), a.getHolderName(), a.getAccountType(),
                a.getBalance(), a.getStatus());
        }
    }

    private void closeAccount() {
        System.out.println("\n-- Close Account --");
        String acc = prompt("Account number : ");
        String pin = prompt("PIN            : ");
        boolean ok = service.closeAccount(acc, pin);
        System.out.println(ok ? "Account closed." : "Could not close account.");
    }

    // ---------------- Helpers ----------------

    private String prompt(String label) {
        System.out.print(label);
        return in.nextLine().trim();
    }
}
