# Bank Management System

A robust, console-based banking application built with **Java 17**, **JDBC**, and **MySQL**, demonstrating a clean **layered architecture** (UI → Service → DAO → Database).

## Features

- Open new savings / current accounts (with PIN protection)
- Deposit, withdraw, and transfer funds (atomic, ACID-compliant)
- Real-time balance enquiry
- Mini statement of recent transactions
- List / close accounts
- Row-level locking (`SELECT ... FOR UPDATE`) prevents lost-update / race conditions
- Deterministic lock ordering on transfers prevents deadlocks
- SHA-256 hashed PINs (never stored in plain text)
- All money movements wrapped in JDBC transactions (`commit` / `rollback`)

## Project Structure

```
bank-management-system/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/bank/
    │   ├── ui/         Console UI       (ConsoleUI, Main)
    │   ├── service/    Business logic   (BankService)
    │   ├── dao/        JDBC persistence (AccountDAO, TransactionDAO + Impls)
    │   ├── model/      Domain objects   (Account, Transaction)
    │   └── util/       Helpers          (DBConnection, PasswordUtil)
    └── resources/
        ├── db.properties   JDBC settings
        └── schema.sql      MySQL DDL
```

## Architecture

```
 ┌──────────────────────┐
 │   ConsoleUI (UI)     │  ← reads input, renders output
 └─────────┬────────────┘
           ▼
 ┌──────────────────────┐
 │  BankService          │  ← business rules, transaction control
 └─────────┬────────────┘
           ▼
 ┌──────────────────────┐
 │  AccountDAO /         │  ← pure JDBC persistence
 │  TransactionDAO       │
 └─────────┬────────────┘
           ▼
 ┌──────────────────────┐
 │     MySQL 8.x         │
 └──────────────────────┘
```

## Setup

### 1. Create the database

```bash
mysql -u root -p < src/main/resources/schema.sql
```

### 2. Configure credentials

Edit `src/main/resources/db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/bank_db?useSSL=false&serverTimezone=UTC
db.username=YOUR_USER
db.password=YOUR_PASSWORD
db.driver=com.mysql.cj.jdbc.Driver
```

### 3. Build & run (Maven)

```bash
mvn clean package
java -jar target/bank-management-system.jar
```

### 3b. Build & run (without Maven)

Download `mysql-connector-j-8.4.0.jar` into `lib/`, then:

```bash
# compile
javac -d out -cp lib/mysql-connector-j-8.4.0.jar \
      $(find src/main/java -name "*.java")

# include resources on the classpath
cp -r src/main/resources/* out/

# run
java -cp out:lib/mysql-connector-j-8.4.0.jar com.bank.ui.Main
```

(On Windows replace `:` with `;` in the classpath.)

## Sample Session

```
=========================================
   BANK MANAGEMENT SYSTEM (JDBC + MySQL)
=========================================

------------- MAIN MENU -----------------
 1. Open new account
 2. Deposit
 3. Withdraw
 4. Transfer funds
 5. Check balance
 6. Mini statement (last 10 txns)
 7. List all accounts
 8. Close account
 0. Exit
Choose an option: 1

-- Open New Account --
Holder name      : Alice Walker
Email            : alice@example.com
Phone            : +1-202-555-0100
Type [S]avings/[C]urrent: S
Initial deposit  : 1000
Set 4-digit PIN  : 1234

Account opened successfully!
  Account Number : 100147382911
  Holder         : Alice Walker
  Type           : SAVINGS
  Balance        : 1000
```

## Design Highlights

| Concern             | Implementation |
|---------------------|----------------|
| Layering            | UI → Service → DAO; each layer depends only on the one directly below it. |
| Persistence         | Plain JDBC + `PreparedStatement` (no ORM) — protects against SQL injection. |
| Transactions        | `Connection.setAutoCommit(false)` + commit / rollback in `BankService`. |
| Concurrency         | `SELECT ... FOR UPDATE` row locks; deterministic lock ordering for transfers. |
| Money               | `BigDecimal` end-to-end (never `double`/`float` for currency). |
| Security            | PINs SHA-256 hashed; account status enforced (`ACTIVE`/`FROZEN`/`CLOSED`). |
| Auditability        | Every balance change writes a `transactions` row with `reference_no`. |

## Extending the System

- Replace SHA-256 with BCrypt / Argon2 (`org.mindrot:jbcrypt`).
- Introduce a connection pool (HikariCP) for production load.
- Add an `Admin` role for freezing accounts and reversing transactions.
- Expose the service layer over REST (Spring Boot) or gRPC without rewriting business logic.
