# Transaction Service

## Overview
Transaction Service is a microservice responsible for orchestrating all monetary transactions in the digital wallet system. It handles deposits, withdrawals, and transfers between wallets by communicating with the Wallet Service to update balances. This service maintains a complete transaction history and ensures data consistency across operations.

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
- **Spring Boot 4.0.0** - Framework for building production-ready applications
- **Spring Data JPA** - Data persistence and ORM
- **H2 Database** - In-memory database for development and testing
- **RestTemplate** - For synchronous HTTP communication with Wallet Service
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

**Transaction Service Responsibilities:**
- Orchestrates complex business operations (deposits, withdrawals, transfers)
- Maintains complete transaction history
- Validates business rules before operations
- Coordinates multiple wallet updates (for transfers)
- Records both successful and failed transactions

**Why This Separation Makes Sense:**

1. **Different Business Domains**
   - Wallets = State management (what exists and its current balance)
   - Transactions = Operations and history (what happened and when)

2. **Scalability**
   - Transaction processing is computationally heavier
   - Can scale Transaction Service independently based on transaction volume
   - Wallet Service scales based on user lookups

3. **Data Isolation**
   - Transaction history can grow very large (millions of records)
   - Wallet data remains small (one record per wallet)
   - Separate databases prevent transaction history from impacting wallet lookups

4. **Fault Tolerance**
   - If Transaction Service fails, users can still view their balances
   - If Wallet Service fails, Transaction Service can queue operations
   - Failure in one service doesn't cascade to the other

## Dependencies

### Core Dependencies
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
**Purpose**: Provides REST API capabilities and Spring MVC. Includes embedded Tomcat server for running the service independently.
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```
**Purpose**: Simplifies database operations with JPA/Hibernate. Provides transaction management which is critical for maintaining data consistency during money movements.
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
</dependency>
```
**Purpose**: In-memory database for development and testing. No external database setup required. In production, would use PostgreSQL or MySQL with proper transaction isolation levels.
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>
```
**Purpose**: Eliminates boilerplate code (getters, setters, constructors). Particularly useful for DTOs where we have many data transfer objects with similar structures.
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```
**Purpose**: Provides request validation using annotations (@NotNull, @Positive, etc.). Critical for financial transactions to ensure amount is positive, wallet IDs are provided, etc.

### Additional Configuration

**RestTemplate Bean**:
```java
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```
**Purpose**: Enables synchronous HTTP communication with Wallet Service. Used by `WalletClient` to make REST calls for wallet operations.

## API Endpoints

### 1. Deposit Money
Deposits money into a wallet from an external source (simulated as "SYSTEM").

**Endpoint**: `POST /api/transactions/deposit`

**Request Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
    "walletId": "WALLET-550e8400-e29b-41d4-a716-446655440000",
    "amount": 5000.00,
    "description": "Initial deposit"
}
```

**Field Validations**:
- `walletId`: Required, must not be blank
- `amount`: Required, must be positive
- `description`: Optional

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

**Error Response** (400 Bad Request):
```json
{
    "timestamp": "2025-11-27T14:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Deposit failed: Wallet not found: WALLET-invalid-id"
}
```

**Error Response** (503 Service Unavailable):
```json
{
    "timestamp": "2025-11-27T14:30:00",
    "status": 503,
    "error": "Service Unavailable",
    "message": "Failed to update wallet balance: Connection refused"
}
```

---

### 2. Withdraw Money
Withdraws money from a wallet to an external destination (simulated as "SYSTEM").

**Endpoint**: `POST /api/transactions/withdraw`

**Request Body**:
```json
{
    "walletId": "WALLET-550e8400-e29b-41d4-a716-446655440000",
    "amount": 500.00,
    "description": "ATM withdrawal"
}
```

**Field Validations**:
- `walletId`: Required, must not be blank
- `amount`: Required, must be positive
- `description`: Optional

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

**Error Response** (400 Bad Request - Insufficient Balance):
```json
{
    "timestamp": "2025-11-27T15:00:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Withdrawal failed: Insufficient balance in source wallet"
}
```

---

### 3. Transfer Money
Transfers money from one wallet to another wallet.

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

**Field Validations**:
- `fromWalletId`: Required, must not be blank
- `toWalletId`: Required, must not be blank
- `amount`: Required, must be positive
- `description`: Optional

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

**Error Response** (400 Bad Request):
```json
{
    "timestamp": "2025-11-27T15:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Transfer failed: Insufficient balance in source wallet"
}
```

**Transaction Flow for Transfers**:
1. Verify both wallets exist
2. Check source wallet has sufficient balance
3. Deduct amount from source wallet
4. Add amount to destination wallet
5. Record transaction with SUCCESS status
6. If any step fails, record transaction with FAILED status

---

### 4. Get Transaction by ID
Retrieves details of a specific transaction.

**Endpoint**: `GET /api/transactions/{transactionId}`

**Path Parameters**:
- `transactionId` (string, required): The unique transaction identifier

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

**Error Response** (400 Bad Request):
```json
{
    "timestamp": "2025-11-27T16:00:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Transaction not found: TXN-invalid-id"
}
```

---

### 5. Get Wallet Transaction History
Retrieves all transactions associated with a specific wallet (both sent and received).

**Endpoint**: `GET /api/transactions/wallet/{walletId}`

**Path Parameters**:
- `walletId` (string, required): The wallet identifier

**Example Request**:
```
GET http://localhost:8081/api/transactions/wallet/WALLET-550e8400-e29b-41d4-a716-446655440000
```

**Success Response** (200 OK):
```json
[
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
    },
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
    },
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
]
```

**Response when no transactions found** (200 OK):
```json
[]
```

## Running the Service

### Prerequisites
- Java 17 or higher installed
- Maven installed (or use included Maven wrapper)
- **Wallet Service must be running on port 8080**
- Port 8081 available for Transaction Service

### Steps to Run

**IMPORTANT**: Start Wallet Service first before starting Transaction Service.

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
```bash
# In another terminal, verify Wallet Service is up
curl http://localhost:8080/actuator/health
```

4. **Run the Transaction Service**
```bash
./mvnw spring-boot:run
```

Or run directly:
```bash
java -jar target/transaction-service-0.0.1-SNAPSHOT.jar
```

5. **Verify it's running**
```bash
curl http://localhost:8081/actuator/health
```

The service will start on `http://localhost:8081`

### H2 Database Console
Access the in-memory database console for debugging:
- URL: `http://localhost:8081/h2-console`
- JDBC URL: `jdbc:h2:mem:transactiondb`
- Username: `sa`
- Password: (leave empty)

### Running Both Services Together

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
| description     | VARCHAR(255) | NULLABLE              | Optional transaction description     |

**Indexes**:
- Primary key index on `id`
- Unique index on `transaction_id`
- Composite index on `from_wallet_id` and `to_wallet_id` for history queries

### Transaction Types
- **DEPOSIT**: Money coming from external source (fromWalletId = "SYSTEM")
- **WITHDRAWAL**: Money going to external destination (toWalletId = "SYSTEM")
- **TRANSFER**: Money moving between two wallets

### Transaction Status
- **SUCCESS**: Transaction completed successfully
- **FAILED**: Transaction failed (still recorded for audit purposes)

## Testing with Postman

### Complete End-to-End Test Scenario

**Step 1: Create Two Wallets** (Wallet Service - Port 8080)
```
POST http://localhost:8080/api/wallets
Content-Type: application/json

{
    "ownerName": "Alice Johnson"
}
```

Save Alice's `walletId` from response.
```
POST http://localhost:8080/api/wallets
Content-Type: application/json

{
    "ownerName": "Bob Smith"
}
```

Save Bob's `walletId` from response.

---

**Step 2: Deposit Money to Alice** (Transaction Service - Port 8081)
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

**Step 3: Verify Alice's Balance** (Wallet Service)
```
GET http://localhost:8080/api/wallets/{{aliceWalletId}}
```
Expected balance: 10000.00

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

**Step 6: Verify Final Balances** (Wallet Service)

Alice's balance:
```
GET http://localhost:8080/api/wallets/{{aliceWalletId}}
```
Expected: 6000.00 (10000 - 1000 - 3000)

Bob's balance:
```
GET http://localhost:8080/api/wallets/{{bobWalletId}}
```
Expected: 3000.00

---

**Step 7: Check Transaction History** (Transaction Service)

Alice's transactions:
```
GET http://localhost:8081/api/transactions/wallet/{{aliceWalletId}}
```
Should show: 1 deposit, 1 withdrawal, 1 transfer (as sender)

Bob's transactions:
```
GET http://localhost:8081/api/transactions/wallet/{{bobWalletId}}
```
Should show: 1 transfer (as receiver)

### Error Scenario Tests

**Test 1: Insufficient Balance**
```
POST http://localhost:8081/api/transactions/withdraw
Content-Type: application/json

{
    "walletId": "{{aliceWalletId}}",
    "amount": 999999.00,
    "description": "Too much"
}
```
Expected: 400 Bad Request with error message

**Test 2: Invalid Wallet**
```
POST http://localhost:8081/api/transactions/deposit
Content-Type: application/json

{
    "walletId": "INVALID-WALLET-ID",
    "amount": 100.00,
    "description": "Should fail"
}
```
Expected: 400 Bad Request - wallet not found

**Test 3: Negative Amount Validation**
```
POST http://localhost:8081/api/transactions/deposit
Content-Type: application/json

{
    "walletId": "{{aliceWalletId}}",
    "amount": -100.00,
    "description": "Negative"
}
```
Expected: 400 Bad Request - validation error

## Inter-Service Communication

### How Transaction Service Communicates with Wallet Service

Transaction Service uses **RestTemplate** to make synchronous HTTP calls to Wallet Service.

**WalletClient Class**:
```java
@Component
public class WalletClient {
    private final RestTemplate restTemplate;
    
    @Value("${wallet.service.url}")
    private String walletServiceUrl; // http://localhost:8080
    
    // Get wallet details
    public WalletResponse getWallet(String walletId) {
        String url = walletServiceUrl + "/api/wallets/" + walletId;
        return restTemplate.getForEntity(url, WalletResponse.class).getBody();
    }
    
    // Update wallet balance
    public WalletResponse updateBalance(String walletId, BalanceUpdateRequest request) {
        String url = walletServiceUrl + "/api/wallets/" + walletId + "/balance";
        restTemplate.put(url, request);
        return getWallet(walletId);
    }
}
```

### Communication Flow Example (Deposit)
```
1. Client → Transaction Service
   POST /api/transactions/deposit
   { walletId: "WALLET-123", amount: 5000 }

2. Transaction Service → Wallet Service
   GET /api/wallets/WALLET-123
   (Verify wallet exists)

3. Transaction Service → Wallet Service
   PUT /api/wallets/WALLET-123/balance
   { amount: 5000 }

4. Transaction Service → Database
   Save transaction record (SUCCESS)

5. Transaction Service → Client
   Return transaction details
```

### Error Handling in Inter-Service Calls

**Scenario: Wallet Service is Down**
- Transaction Service catches connection errors
- Records transaction with FAILED status
- Returns `503 Service Unavailable` to client
- Error message: "Failed to update wallet balance: Connection refused"

**Scenario: Wallet Not Found**
- Wallet Service returns `404 Not Found`
- Transaction Service catches exception
- Records transaction with FAILED status
- Returns `400 Bad Request` to client


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

# Wallet Service Configuration
wallet.service.url=http://localhost:8080
```

**Key Configuration**:
- `wallet.service.url`: Points to Wallet Service for inter-service communication

## Future Enhancements

### Security
- **API Authentication**: JWT tokens for service-to-service communication
- **Rate Limiting**: Prevent abuse of transaction endpoints
- **Amount Limits**: Daily/transaction amount limits
- **Fraud Detection**: Pattern recognition for suspicious transactions

### Production Readiness
- **Replace H2**: Use PostgreSQL with proper transaction isolation
- **API Versioning**: Support multiple API versions

## Common Issues & Troubleshooting

### Issue: "Connection refused" when calling Wallet Service
**Solution**: Ensure Wallet Service is running on port 8080 before starting Transaction Service

### Issue: Transaction records FAILED status
**Check**: 

1. Wallet balance is sufficient for withdrawal/transfer

### Issue: Port 8081 already in use
**Solution**: 
```bash
# Find process using port 8081
lsof -i :8081

# Kill the process
kill -9 <PID>
```
