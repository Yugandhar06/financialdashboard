# Finance Dashboard Backend

> A production-grade REST API for a multi-role financial dashboard — built with Spring Boot, MySQL, and JWT authentication.

---

## 📋 Table of Contents

- [Project Overview](#project-overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Features Implemented](#features-implemented)
- [API Endpoints](#api-endpoints)
- [Role-Based Access Control](#role-based-access-control)
- [Database Schema](#database-schema)
- [Security Design](#security-design)
- [Setup & Running Locally](#setup--running-locally)
- [Testing the API](#testing-the-api)
- [Design Decisions & Assumptions](#design-decisions--assumptions)
- [Optional Enhancements Implemented](#optional-enhancements-implemented)
- [Project Structure](#project-structure)

---

## Project Overview

This is a backend REST API for a **Finance Dashboard System** where users with different roles interact with financial records based on their access level. The system supports:

- **User authentication and registration** with JWT tokens
- **Role-based dashboards** (Viewer, Analyst, Admin) that return different levels of data
- **Full CRUD on financial transactions** (income/expense records)
- **Dashboard analytics** — real-time aggregations, category breakdowns, and monthly trends
- **Audit trail** — every create, update, and delete is logged with who changed what and when
- **Advanced Insights** — spending trend predictions, budget tracking, and smart recommendations

The frontend is built with React + Vite and connects to this backend over HTTP on `localhost:8080`.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.x |
| Security | Spring Security + JWT (JJWT 0.11.5) |
| ORM | Spring Data JPA / Hibernate |
| Database | MySQL 8.0 |
| Build Tool | Maven |
| Password Hashing | BCrypt |
| Validation | Jakarta Bean Validation |
| Boilerplate Reduction | Lombok |

---

## Architecture

The application follows a clean layered architecture:

```
HTTP Request
    │
    ▼
JwtAuthFilter          ← validates Bearer token on every request
    │
    ▼
SecurityConfig         ← URL-level role enforcement (Layer 1)
    │
    ▼
Controller             ← @PreAuthorize role checks (Layer 2)
    │
    ▼
Service                ← all business logic lives here
    │
    ▼
Repository             ← JPA + custom @Query aggregations
    │
    ▼
MySQL Database
```

**Two-layer access control** ensures security is enforced at both the URL routing level and the method level — so even if URL rules are misconfigured, method-level `@PreAuthorize` annotations act as a second line of defense.

---

## Features Implemented

### ✅ Core Requirements

| Requirement | Status | Key Details |
|---|---|---|
| User & Role Management | ✅ Complete | 3 roles: VIEWER, ANALYST, ADMIN |
| Financial Records CRUD | ✅ Complete | Create, Read, Update, Soft Delete |
| Dashboard Summary APIs | ✅ Complete | Role-specific dashboards with real aggregations |
| Access Control Logic | ✅ Complete | Two-layer RBAC (URL + method level) |
| Validation & Error Handling | ✅ Complete | Jakarta validation + centralized GlobalExceptionHandler |
| Data Persistence | ✅ Complete | MySQL with Hibernate DDL auto-update |

### ✅ Optional Enhancements

| Enhancement | Status |
|---|---|
| JWT Authentication | ✅ Implemented |
| Pagination | ✅ Implemented |
| Search & Filtering | ✅ Implemented |
| Soft Delete | ✅ Implemented |
| Audit Log | ✅ Implemented (bonus — not in original spec) |
| Advanced Insights Engine | ✅ Implemented (bonus — not in original spec) |

---

## API Endpoints

### 🔓 Public (no token required)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new user (defaults to VIEWER role) |
| POST | `/api/auth/login` | Login and receive a JWT token |

### 👁️ Viewer (VIEWER + ANALYST + ADMIN)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/viewer/dashboard` | Basic profile, current balance, recent transactions |

### 📊 Analyst (ANALYST + ADMIN)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/analyst/dashboard` | Viewer data + category totals + 12-month trends |
| GET | `/api/analyst/insights` | Spending predictions, budget status, recommendations |

### 🛠️ Admin — Transactions

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/admin/transactions` | Create a new financial record |
| GET | `/api/admin/transactions` | List all transactions (paginated + filtered) |
| GET | `/api/admin/transactions/{id}` | Get a single transaction |
| PUT | `/api/admin/transactions/{id}` | Partial update (only provided fields change) |
| DELETE | `/api/admin/transactions/{id}` | Soft delete (sets `is_deleted = true`) |

**Query parameters for listing transactions:**
```
?type=INCOME         → filter by type (INCOME or EXPENSE)
?category=salary     → partial, case-insensitive category search
?startDate=2026-01-01
?endDate=2026-03-31
?page=0&size=10      → pagination (max size: 100)
```

### 🛠️ Admin — Users

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/admin/users` | List all users |
| GET | `/api/admin/users/{id}` | Get a single user |
| PATCH | `/api/admin/users/{id}/role` | Change a user's role |
| PATCH | `/api/admin/users/{id}/status` | Activate or deactivate a user |
| DELETE | `/api/admin/users/{id}` | Hard delete a user |

### 🛠️ Admin — Audit Log

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/admin/audit/transactions/{id}` | Full change history for a transaction |
| GET | `/api/admin/audit/users/{id}` | Full change history for a user |
| GET | `/api/admin/audit/recent?hours=24` | All changes in the last N hours |
| GET | `/api/admin/audit/deleted` | All soft-deleted records |
| GET | `/api/admin/audit/user?email=...` | All changes made by a specific user |

### 🛠️ Admin — Dashboard

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/admin/dashboard` | Analyst data + system-wide user/transaction counts |

---

## Role-Based Access Control

```
VIEWER
  └── GET /api/viewer/dashboard

ANALYST (inherits VIEWER access)
  ├── GET /api/analyst/dashboard
  ├── GET /api/analyst/insights
  └── GET /api/admin/transactions (read only)

ADMIN (inherits ANALYST access)
  ├── Full CRUD on /api/admin/transactions
  ├── Full management on /api/admin/users
  ├── GET /api/admin/dashboard
  └── Full access to /api/admin/audit
```

**Business rules enforced in the service layer:**
- An admin cannot demote their own role (prevents self-lockout)
- An admin cannot deactivate their own account
- The last admin in the system cannot be demoted
- New users always register as VIEWER (principle of least privilege)
- Inactive users are blocked at authentication time

---

## Database Schema

### `users` table

| Column | Type | Notes |
|---|---|---|
| id | BIGINT (PK) | Auto-increment |
| name | VARCHAR(100) | |
| email | VARCHAR(150) | Unique |
| password | VARCHAR(255) | BCrypt hash — never plaintext |
| role | ENUM | VIEWER, ANALYST, ADMIN |
| status | ENUM | ACTIVE, INACTIVE |
| created_at | DATETIME | Set by Hibernate |
| updated_at | DATETIME | Auto-updated by Hibernate |

### `transactions` table

| Column | Type | Notes |
|---|---|---|
| id | BIGINT (PK) | Auto-increment |
| user_id | BIGINT (FK) | References `users.id` |
| amount | DECIMAL(15,2) | **Never Float/Double** — financial precision |
| type | ENUM | INCOME, EXPENSE |
| category | VARCHAR(100) | Free-form string |
| date | DATE | Business date (when it occurred) |
| notes | TEXT | Optional description |
| is_deleted | BOOLEAN | Soft delete flag (default: false) |
| created_at | DATETIME | |
| updated_at | DATETIME | |

**Indexes:** `idx_type`, `idx_category`, `idx_date`, `idx_deleted` — for fast dashboard filtering.

### `audit_logs` table

| Column | Type | Notes |
|---|---|---|
| id | BIGINT (PK) | |
| entity_id | BIGINT | ID of the changed record |
| entity_type | VARCHAR | TRANSACTION or USER |
| action | VARCHAR | CREATE, UPDATE, DELETE |
| changed_by | VARCHAR | Email of the user who made the change |
| field_name | VARCHAR | Which field was updated (UPDATE only) |
| old_value | TEXT | Previous value |
| new_value | TEXT | New value |
| timestamp | DATETIME | Immutable, set on creation |

---

## Security Design

### Authentication Flow

```
1. User POSTs to /api/auth/login with email + password
2. Spring's AuthenticationManager verifies BCrypt password
3. If valid and ACTIVE → JwtUtil generates a signed token (24hr expiry)
4. Client stores token and sends it as: Authorization: Bearer <token>
5. JwtAuthFilter extracts + validates the token on every subsequent request
6. User's role is loaded from DB and set in SecurityContext
7. @PreAuthorize checks then enforce role-based access
```

### Why BigDecimal for money?

`Float` and `Double` use binary floating-point, which introduces rounding errors:
```java
// This actually evaluates to 0.30000000000000004 in Java
0.1 + 0.2 == 0.3  // false!
```

`BigDecimal` is exact decimal arithmetic. The DB column is `DECIMAL(15,2)` which stores up to `999,999,999,999,999.99` — sufficient for any financial application.

### Soft Delete

Financial records are never physically deleted. Instead, `is_deleted = true` is set. All queries automatically filter `WHERE is_deleted = false`. This preserves the audit trail and supports compliance requirements.

---

## Setup & Running Locally

### Prerequisites

- Java 17+
- MySQL 8.0+
- Maven 3.8+ (or use the included `./mvnw` wrapper)

### Step 1: Create the database

```sql
CREATE DATABASE dashboard;
```

### Step 2: Configure connection

Edit `backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/dashboard?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=root
```

### Step 3: Run the backend

```bash
cd backend

# Linux / Mac
./mvnw spring-boot:run

# Windows
.\mvnw.cmd spring-boot:run
```

The server starts on `http://localhost:8080`.

### Step 4: Create an admin account

Register via the API, then the `DatabaseSeeder` auto-promotes `admin@finance.com` to ADMIN on startup. Or insert directly:

```sql
-- Use a BCrypt generator to hash your password first
INSERT INTO users (name, email, password, role, status)
VALUES ('Admin', 'admin@finance.com', '$2a$10$HASH_HERE', 'ADMIN', 'ACTIVE');
```

### Step 5 (Optional): Run the frontend

```bash
cd frontend
npm install
npm run dev   # Vite server on http://localhost:5173
```

---

## Testing the API

### Quick start with cURL

**Register:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@finance.com","password":"Password123!"}'
```

**Login and save token:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@finance.com","password":"Password123!"}'
# Copy the "token" value from the response
```

**Access viewer dashboard:**
```bash
curl http://localhost:8080/api/viewer/dashboard \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**Create a transaction (Admin only):**
```bash
curl -X POST http://localhost:8080/api/admin/transactions \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount":5000.00,"type":"INCOME","category":"Salary","date":"2026-01-15"}'
```

### Consistent API response format

Every response — success or error — uses the same shape:

```json
{
  "success": true,
  "message": "Transaction created successfully",
  "data": { ... },
  "timestamp": "2026-04-05T10:30:00"
}
```

### HTTP Status Codes used

| Code | When |
|---|---|
| 200 | Successful read or update |
| 201 | Resource created |
| 400 | Validation error or invalid business operation |
| 401 | Missing, expired, or invalid JWT |
| 403 | Valid token but insufficient role |
| 404 | Resource not found |
| 409 | Conflict (e.g., email already registered) |

---

## Design Decisions & Assumptions

**Why two layers of access control?**
URL-level rules in `SecurityConfig` handle authentication checks. Method-level `@PreAuthorize` handles authorization. Separating them means a misconfiguration in one layer doesn't expose the other — defense in depth.

**Why soft delete?**
Financial data should never be erased. Soft delete keeps the audit trail intact, supports recovery from accidental deletion, and prevents dashboard history from silently changing. All queries filter `is_deleted = false` automatically via JPA Specifications.

**Why database-level aggregations?**
Dashboard SUM and GROUP BY queries run in MySQL, not in Java memory. On a system with millions of transactions, loading all rows into Java to sum them would be extremely slow and memory-intensive.

**Why partial updates (PUT behaving like PATCH)?**
The update endpoint only modifies fields that are explicitly provided. Sending `{ "amount": 6000 }` updates only the amount — everything else stays. This is noted as an intentional design tradeoff for better client usability.

**Why are roles stored as strings in MySQL?**
`ENUM('VIEWER','ANALYST','ADMIN')` is self-describing. You can read the database directly without a lookup table, which helps during debugging and auditing.

**Assumption — Admin bootstrap:**
The system bootstraps the first admin by auto-promoting `admin@finance.com` on startup via `DatabaseSeeder`. In production this would be handled differently, but for a local development and review context, this is the simplest approach.

---

## Optional Enhancements Implemented

### Audit Trail (Bonus Feature)

Every CREATE, UPDATE, and DELETE on transactions and users is logged to an `audit_logs` table. The `AuditLogService` is called directly from `TransactionService` and `UserService` after each state change. Admins can query the full change history for any entity, filter by time window, or see all deletes.

### Insights Engine (Bonus Feature)

`InsightsService` provides:
- **Spending trend analysis** — compares this month to last month, calculates percentage change, and determines direction (UP / DOWN / STABLE)
- **Next month prediction** — simple average of the last two months
- **Category analysis** — top and lowest spending categories with trend comparison
- **Budget status** — compares actual spending against a $5,000/month budget model per category
- **Smart recommendations** — generates actionable suggestions, warnings, and saving opportunities based on the computed trends

---

## Project Structure

```
backend/src/main/java/com/finance/dashboard/
├── DashboardApplication.java
├── config/
│   ├── SecurityConfig.java        ← JWT filter chain, two-layer RBAC setup
│   ├── CorsConfig.java
│   └── DatabaseSeeder.java        ← auto-promotes admin on startup
├── controller/
│   ├── AuthController.java
│   ├── ViewerController.java
│   ├── AnalystController.java
│   ├── AdminDashboardController.java
│   ├── TransactionController.java
│   ├── UserController.java
│   └── AuditLogController.java
├── service/
│   ├── AuthService.java
│   ├── DashboardService.java      ← aggregations, trend calculations
│   ├── TransactionService.java    ← CRUD + soft delete + audit logging
│   ├── TransactionSpecification.java ← dynamic JPA filtering
│   ├── UserService.java           ← role/status management + business rules
│   ├── InsightsService.java       ← spending predictions, budget tracking
│   └── AuditLogService.java
├── repository/
│   ├── TransactionRepository.java ← custom @Query aggregations
│   ├── UserRepository.java
│   └── AuditLogRepository.java
├── entity/
│   ├── Transaction.java
│   ├── User.java
│   └── AuditLog.java
├── dto/
│   ├── request/                   ← validated input DTOs
│   └── response/                  ← response DTOs (never expose entity directly)
├── enums/
│   ├── Role.java
│   ├── TransactionType.java
│   └── UserStatus.java
├── exception/
│   ├── GlobalExceptionHandler.java ← centralized error handling
│   ├── BadRequestException.java
│   ├── ResourceNotFoundException.java
│   └── ConflictException.java
└── security/
    ├── JwtUtil.java
    ├── JwtAuthFilter.java
    └── CustomUserDetailsService.java
```

---

## Live Tested Scenarios

| Scenario | Expected | Result |
|---|---|---|
| Register new user | 201 + JWT token | ✅ Pass |
| Login with valid credentials | 200 + JWT token | ✅ Pass |
| VIEWER accesses viewer dashboard | 200 + profile + balance | ✅ Pass |
| VIEWER tries to POST a transaction | 403 Forbidden | ✅ Pass |
| ADMIN creates a transaction | 201 + transaction object | ✅ Pass |
| List transactions with filters | Paginated results | ✅ Pass |
| Soft delete a transaction | Sets is_deleted = true | ✅ Pass |
| Duplicate email registration | 409 Conflict | ✅ Pass |
| Negative amount on transaction | 400 Bad Request | ✅ Pass |
| Admin demotes last admin | 400 Bad Request | ✅ Pass |
| Expired/invalid JWT | 403 Forbidden | ✅ Pass |
