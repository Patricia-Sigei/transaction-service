# Transaction Service

## Overview
Transaction Service is a microservice responsible for handling all monetary transactions in the digital wallet system. It manages deposits, withdrawals, and transfers between wallets by communicating with the Wallet Service. This service maintains transaction history and records both successful and failed operations.

## Table of Contents
- [Technologies Used](#technologies-used)
- [Project Structure](#project-structure)
- [Architecture Decisions](#architecture-decisions)
- [Dependencies](#dependencies)
- [API Endpoints](#api-endpoints)
- [Running the Service](#running-the-service)
- [Database Schema](#database-schema)
- [Testing with Postman](#testing-with-postman)
- [Inter-Service Communication](#inter-service-communication)

## Technologies Used
- **Java 17** - LTS version with modern language features
- **Spring Boot 4.0.0** - Framework for building applications
- **Spring Data JPA** - Data persistence and ORM
- **H2 Database** - In-memory database for development and testing
- **RestTemplate** - For HTTP communication with Wallet Service
- **Lombok** - Reduces boilerplate code
- **Maven** - Build and dependency management

## Project Structure
```
transaction-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/wallet/transaction/
│   │   │       ├── client/
│   │   │       │   └── WalletClient.java
│   │   │       ├── config/
│   │   │       │   └── RestTemplateConfig.java
│   │   │       ├── controller/
│   │   │       │   └── TransactionController.java
│   │   │       ├── dto/
│   │   │       │   ├── DepositRequest.java
│   │   │       │   ├── WithdrawalRequest.java
│   │   │       │   ├── TransferRequest.java
│   │   │       │   ├── TransactionResponse.java
│   │   │       │   ├── BalanceUpdateRequest.java
│   │   │       │   └── WalletResponse.java
│   │   │       ├── entity/
│   │   │       │   ├── Transaction.java
│   │   │       │   ├── TransactionType.java
│   │   │       │   └── TransactionStatus.java
│   │   │       ├── exception/
│   │   │       │   ├── GlobalExceptionHandler.java
│   │   │       │   ├── TransactionFailedException.java
│   │   │       │   └── WalletServiceException.java
│   │   │       ├── repository/
│   │   │       │   └── TransactionRepository.java
│   │   │       ├── service/
│   │   │       │   └── TransactionService.java
│   │   │       └── TransactionServiceApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── pom.xml
└── README.md
```

## Architecture Decisions

### Why Transaction Service is Separate from Wallet Service

**Transaction Service:**
- Handles deposits, withdrawals, and transfers
- Keeps transaction history
- Calls Wallet Service to update balances
- Records both successful and failed transactions

**Why separate:**
- Each service has a single, clear responsibility
- Transaction history and wallet data are kept in separate databases
- Services can be deployed and updated independently
- If one service fails, the other can still function

**Service Communication:**
- Transaction Service calls Wallet Service via REST APIs using RestTemplate

## Dependencies

### Core Dependencies
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
**Purpose**: Provides REST API capabilities and Spring MVC. Includes embedded Tomcat server.
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```
**Purpose**: Simplifies database operations with JPA/Hibernate. Handles transaction management for data consistency.
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
</dependency>
```
**Purpose**: In-memory database for development and testing. No external database setup required.
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>
```
**Purpose**: Reduces boilerplate code (getters, setters, constructors).
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```
**Purpose**: Provides request validation using annotations (@NotNull, @Positive, etc.). Ensures transaction amounts are valid.

### Additional Configuration

**RestTemplate Bean**:
```java
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```
**Purpose**: Enables HTTP communication with Wallet Service for updating balances.

## API Endpoints

### 1. Deposit Money
Deposits money into a wallet from an external source (shown as "SYSTEM").

**Endpoint**: `POST /api/transactions/deposit`

**Request Body**:
```json
{
    "walletId": "WALLET-550e8400-e29b-41d4-a716-446655440000",
    "amount": 5000.00,
    "description": "Initial deposit"
}
```

**Success Response** (201 Created):
```json
{
    "id": 1,
    "transactionId": "TXN-a36adc57-5448-427c-90ab-dd65527c326f",
    "fromWalletId": "SYSTEM",
    "toWalletId": "WALLET-550e8400-e29b-41d4-a716-446655440000",
    "amount": 5000.00,
    "type": "DEPOSIT",
    "status": "SUCCESS",
    "timestamp": "2025-11-27T14:30:00",
    "description": "Initial deposit"
}
```

---

### 2. Withdraw Money
Withdraws money from a wallet.

**Endpoint**: `POST /api/transactions/withdraw`

**Request Body**:
```json
{
    "walletId": "WALLET-550e8400-e29b-41d4-a716-446655440000",
    "amount": 500.00,
    "description": "ATM withdrawal"
}
```

**Success Response** (201 Created):
```json
{
    "id": 2,
    "transactionId": "TXN-b47bec68-6559-538d-b827-ee76656551af",
    "fromWalletId": "WALLET-550e8400-e29b-41d4-a716-446655440000",
    "toWalletId": "SYSTEM",
    "amount": 500.00,
    "type": "WITHDRAWAL",
    "status": "SUCCESS",
    "timestamp": "2025-11-27T15:00:00",
    "description": "ATM withdrawal"
}
```

---

### 3. Transfer Money
Transfers money from one wallet to another.

**Endpoint**: `POST /api/transactions/transfer`

**Request Body**:
```json
{
    "fromWalletId": "WALLET-550e8400-e29b-41d4-a716-446655440000",
    "toWalletId": "WALLET-d61c8516-b0a3-47f3-809e-24175ebc02d5",
    "amount": 2000.00,
    "description": "Payment for services"
}
```

**Success Response** (201 Created):
```json
{
    "id": 3,
    "transactionId": "TXN-c58cfd79-7660-649e-c938-ff87767662b0",
    "fromWalletId": "WALLET-550e8400-e29b-41d4-a716-446655440000",
    "toWalletId": "WALLET-d61c8516-b0a3-47f3-809e-24175ebc02d5",
    "amount": 2000.00,
    "type": "TRANSFER",
    "status": "SUCCESS",
    "timestamp": "2025-11-27T15:30:00",
    "description": "Payment for services"
}
```

**How transfers work:**
1. Verify both wallets exist
2. Check source wallet has enough balance
3. Deduct amount from source wallet
4. Add amount to destination wallet
5. Record transaction

---

### 4. Get Transaction by ID
Retrieves details of a specific transaction.

**Endpoint**: `GET /api/transactions/{transactionId}`

**Example Request**:
```
GET http://localhost:8081/api/transactions/TXN-a36adc57-5448-427c-90ab-dd65527c326f
```

**Success Response** (200 OK):
```json
{
    "id": 1,
    "transactionId": "TXN-a36adc57-5448-427c-90ab-dd65527c326f",
    "fromWalletId": "SYSTEM",
    "toWalletId": "WALLET-550e8400-e29b-41d4-a716-446655440000",
    "amount": 5000.00,
    "type": "DEPOSIT",
    "status": "SUCCESS",
    "timestamp": "2025-11-27T14:30:00",
    "description": "Initial deposit"
}
```

---

### 5. Get Wallet Transaction History
Retrieves all transactions for a specific wallet.

**Endpoint**: `GET /api/transactions/wallet/{walletId}`

**Example Request**:
```
GET http://localhost:8081/api/transactions/wallet/WALLET-550e8400-e29b-41d4-a716-446655440000
```

**Success Response** (200 OK):
Returns an array of all transactions where the wallet was either sender or receiver.

## Running the Service

### Prerequisites
- Java 17 or higher installed
- Maven installed (or use included Maven wrapper)
- **Wallet Service must be running on port 8080**
- Port 8081 available

### Steps to Run

**IMPORTANT**: Start Wallet Service first.

1. **Clone the repository**
```bash
git clone https://github.com/Patricia-Sigei/transaction-service.git
cd transaction-service
```

2. **Build the project**
```bash
./mvnw clean install
```

3. **Ensure Wallet Service is running**
Verify at: http://localhost:8080

4. **Run Transaction Service**
```bash
./mvnw spring-boot:run
```

The service will start on `http://localhost:8081`

### H2 Database Console
- URL: `http://localhost:8081/h2-console`
- JDBC URL: `jdbc:h2:mem:transactiondb`
- Username: `sa`
- Password: (leave empty)

### Running Both Services

**Terminal 1 - Wallet Service:**
```bash
cd digital-wallet-system
./mvnw spring-boot:run
```

**Terminal 2 - Transaction Service:**
```bash
cd transaction-service
./mvnw spring-boot:run
```

## Database Schema

### Transaction Table
| Column          | Type         | Constraints           | Description                          |
|-----------------|--------------|-----------------------|--------------------------------------|
| id              | BIGINT       | PRIMARY KEY, AUTO     | Internal database ID                 |
| transaction_id  | VARCHAR(255) | UNIQUE, NOT NULL      | Business identifier (UUID)           |
| from_wallet_id  | VARCHAR(255) | NOT NULL              | Source wallet (or "SYSTEM")          |
| to_wallet_id    | VARCHAR(255) | NOT NULL              | Destination wallet (or "SYSTEM")     |
| amount          | DECIMAL(38,2)| NOT NULL              | Transaction amount                   |
| type            | VARCHAR(50)  | NOT NULL              | DEPOSIT, WITHDRAWAL, or TRANSFER     |
| status          | VARCHAR(50)  | NOT NULL              | SUCCESS or FAILED                    |
| timestamp       | TIMESTAMP    | NOT NULL              | Transaction timestamp                |
| description     | VARCHAR(255) | NULLABLE              | Optional description                 |

### Transaction Types
- **DEPOSIT**: Money from external source (fromWalletId = "SYSTEM")
- **WITHDRAWAL**: Money to external destination (toWalletId = "SYSTEM")
- **TRANSFER**: Money between two wallets

### Transaction Status
- **SUCCESS**: Transaction completed
- **FAILED**: Transaction failed (recorded for tracking)

## Testing with Postman

### Complete Test Scenario

**Step 1: Create Two Wallets** (Wallet Service)
```
POST http://localhost:8080/api/wallets
Content-Type: application/json

{
    "ownerName": "Alice Johnson"
}
```
```
POST http://localhost:8080/api/wallets
Content-Type: application/json

{
    "ownerName": "Bob Smith"
}
```

Save both `walletId` values.

---

**Step 2: Deposit to Alice** (Transaction Service)
```
POST http://localhost:8081/api/transactions/deposit
Content-Type: application/json

{
    "walletId": "{{aliceWalletId}}",
    "amount": 10000.00,
    "description": "Initial deposit"
}
```

---

**Step 3: Check Alice's Balance** (Wallet Service)
```
GET http://localhost:8080/api/wallets/{{aliceWalletId}}
```
Expected: 10000.00

---

**Step 4: Withdraw from Alice** (Transaction Service)
```
POST http://localhost:8081/api/transactions/withdraw
Content-Type: application/json

{
    "walletId": "{{aliceWalletId}}",
    "amount": 1000.00,
    "description": "ATM withdrawal"
}
```

---

**Step 5: Transfer from Alice to Bob** (Transaction Service)
```
POST http://localhost:8081/api/transactions/transfer
Content-Type: application/json

{
    "fromWalletId": "{{aliceWalletId}}",
    "toWalletId": "{{bobWalletId}}",
    "amount": 3000.00,
    "description": "Payment to Bob"
}
```

---

**Step 6: Check Final Balances** (Wallet Service)

Alice: Expected 6000.00 (10000 - 1000 - 3000)
Bob: Expected 3000.00

---

**Step 7: View Transaction History** (Transaction Service)
```
GET http://localhost:8081/api/transactions/wallet/{{aliceWalletId}}
```

## Inter-Service Communication

### How Transaction Service Calls Wallet Service

Transaction Service uses **RestTemplate** to make HTTP calls to Wallet Service.

**Example flow for deposit:**
1. Client sends deposit request to Transaction Service
2. Transaction Service calls Wallet Service to verify wallet exists
3. Transaction Service calls Wallet Service to update balance
4. Transaction Service saves transaction record
5. Transaction Service returns response to client

### Error Handling

**If Wallet Service is down:**
- Transaction Service records FAILED transaction
- Returns error message to client

**If wallet doesn't exist:**
- Wallet Service returns 404
- Transaction Service records FAILED transaction
- Returns error to client

## Configuration

### application.properties
```properties
# Application Configuration
spring.application.name=transaction-service
server.port=8081

# Database Configuration
spring.datasource.url=jdbc:h2:mem:transactiondb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Wallet Service URL
wallet.service.url=http://localhost:8080
```

## Common Issues

**"Connection refused" when calling Wallet Service**
- Make sure Wallet Service is running on port 8080 first

**Transaction shows FAILED status**
- Check that wallet has sufficient balance
- Verify wallet ID is correct

**Port 8081 already in use**
```bash
lsof -i :8081
kill -9 <PID>
```

## Contact
Patricia Sigei - [GitHub](https://github.com/Patricia-Sigei)
