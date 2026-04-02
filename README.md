# Finance Dashboard Backend

A production-structured REST API built with **Spring Boot** that powers a role-based finance dashboard. The system handles user authentication via JWT, enforces three-tier role-based access control, manages financial transaction records with full CRUD, and serves aggregated analytics for dashboard consumption.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Project Architecture](#project-architecture)
3. [How the System Works](#how-the-system-works)
4. [Authentication Flow](#authentication-flow)
5. [Role-Based Access Control](#role-based-access-control)
6. [API Reference](#api-reference)
7. [Data Models](#data-models)
8. [Dashboard Analytics](#dashboard-analytics)
9. [Error Handling](#error-handling)
10. [Validation Rules](#validation-rules)
11. [Setup & Running Locally](#setup--running-locally)
12. [Environment Configuration](#environment-configuration)
13. [Design Decisions & Assumptions](#design-decisions--assumptions)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Security | Spring Security + JWT (jjwt) |
| Database | MySQL 8 |
| ORM | Spring Data JPA / Hibernate |
| Build Tool | Maven |
| Utilities | Lombok |

---

## Project Architecture

```
com.finance.dashboard/
│
├── config/
│   ├── SecurityConfig.java            # JWT filter chain, CORS, RBAC URL rules
│   ├── CorsConfig.java                # MVC-level CORS (Vite frontend support)
│   └── DatabaseSeeder.java            # Seeds default admin on startup
│
├── controller/
│   ├── AuthController.java            # POST /api/auth/register, /login
│   ├── ViewerController.java          # GET /api/viewer/dashboard
│   ├── AnalystController.java         # GET /api/analyst/dashboard
│   ├── AdminDashboardController.java  # GET /api/admin/dashboard
│   ├── TransactionController.java     # CRUD /api/admin/transactions
│   └── UserController.java            # CRUD /api/admin/users
│
├── service/
│   ├── AuthService.java               # Register + login business logic
│   ├── TransactionService.java        # Transaction CRUD + partial update
│   ├── DashboardService.java          # Analytics aggregation per role
│   └── UserService.java               # User management + self-lockout guards
│
├── security/
│   ├── JwtAuthFilter.java             # OncePerRequestFilter — validates JWT
│   ├── JwtUtil.java                   # Token generation, validation, claim extraction
│   └── CustomUserDetailsService.java  # Loads user from DB for Spring Security
│
├── repository/
│   ├── TransactionRepository.java     # JPA + custom @Query aggregations
│   └── UserRepository.java            # JPA + email lookup + role queries
│
├── entity/
│   ├── Transaction.java               # Maps to `transactions` table
│   └── User.java                      # Maps to `users` table
│
├── dto/
│   ├── request/                       # Validated inbound payloads
│   └── response/                      # Outbound API shapes (no entity leakage)
│
├── enums/
│   ├── Role.java                      # VIEWER, ANALYST, ADMIN
│   ├── TransactionType.java           # INCOME, EXPENSE
│   └── UserStatus.java                # ACTIVE, INACTIVE
│
├── exception/
│   ├── GlobalExceptionHandler.java    # @RestControllerAdvice — catches everything
│   ├── ResourceNotFoundException.java # 404
│   ├── BadRequestException.java       # 400
│   └── ConflictException.java         # 409
│
└── DashboardApplication.java          # Entry point
```

---

## How the System Works

Every HTTP request passes through the following pipeline:

```
Client Request
      │
      ▼
CorsFilter               — handles preflight OPTIONS requests from frontend
      │
      ▼
JwtAuthFilter            — extracts Bearer token, validates signature + expiry,
      │                    sets authenticated user in SecurityContext
      ▼
SecurityFilterChain      — checks URL-level rules (is this endpoint public?)
      │
      ▼
@PreAuthorize            — checks method-level role rules
      │
      ▼
Controller               — receives request, delegates to service
      │
      ▼
Service                  — runs business logic, enforces rules
      │
      ▼
Repository               — queries database, returns entities
      │
      ▼
DTO Mapping              — converts entity → response DTO (no sensitive fields)
      │
      ▼
ApiResponse<T>           — wraps every response in { success, message, data }
      │
      ▼
Client Response
```

---

## Authentication Flow

### Registration — `POST /api/auth/register`

```
1. Client sends { name, email, password, role? }
2. AuthService checks if email already exists → throws 409 if duplicate
3. Password is hashed with BCrypt (cost factor 10)
4. User saved with role VIEWER by default (principle of least privilege)
   → Admin role cannot be self-assigned through registration
5. JWT generated immediately → user is logged in right after registering
6. Response: { token, tokenType, userId, name, email, role }
```

### Login — `POST /api/auth/login`

```
1. Client sends { email, password }
2. Spring AuthenticationManager verifies credentials:
   → Loads user via CustomUserDetailsService
   → Compares BCrypt hash
   → Checks account is ACTIVE
3. On success → JWT generated and returned
4. On failure → AuthenticationManager throws exception automatically
   → GlobalExceptionHandler converts to 401
   → Message is intentionally vague: "Invalid email or password"
     (Never reveal whether the email exists vs wrong password)
```

### JWT on Every Subsequent Request

```
Client sends: Authorization: Bearer eyJhbGci...

JwtAuthFilter runs on every request:
  1. Extracts token from Authorization header
  2. Parses and validates signature using HMAC-SHA256 secret
  3. Checks token is not expired
  4. Loads user from DB to get current role and status
  5. Sets UsernamePasswordAuthenticationToken in SecurityContext
     with user's ROLE_XXX authority

All @PreAuthorize checks downstream use this SecurityContext.
```

**JWT Payload structure:**
```json
{
  "sub": "user@example.com",
  "iat": 1705312200,
  "exp": 1705398600
}
```

Token lifetime defaults to **24 hours** (configurable via `jwt.expiration.ms`).

---

## Role-Based Access Control

RBAC is enforced at **two independent layers** that work together:

### Layer 1 — URL Level (SecurityConfig)

```java
.requestMatchers("/api/auth/**")    → PUBLIC        (no token needed)
.requestMatchers("/api/viewer/**")  → VIEWER+       (all logged-in users)
.requestMatchers("/api/analyst/**") → ANALYST+      (analyst and admin)
.requestMatchers("/api/admin/**")   → ADMIN only
.anyRequest()                       → authenticated  (any valid token)
```

### Layer 2 — Method Level (@PreAuthorize)

Each controller method declares exactly which roles can call it:

```java
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
@PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
```

### Role Capability Matrix

| Action | VIEWER | ANALYST | ADMIN |
|--------|--------|---------|-------|
| View own dashboard (balance + recent) | ✅ | ✅ | ✅ |
| View analytics + monthly trends | ❌ | ✅ | ✅ |
| View all transactions (paginated) | ❌ | ❌ | ✅ |
| Create / Update / Delete transactions | ❌ | ❌ | ✅ |
| View / Manage all users | ❌ | ❌ | ✅ |
| Change user roles and status | ❌ | ❌ | ✅ |

### Admin Self-Lockout Prevention

Three guards in `UserService` prevent admins from accidentally breaking system access:

```
Guard 1: Admin cannot change their own role
         → "You cannot change your own role. Ask another admin to do this."

Guard 2: Admin cannot deactivate their own account
         → "You cannot change your own account status."

Guard 3: The last admin cannot be demoted
         → "Cannot demote the last admin. Promote another user first."
```

---

## API Reference

### Auth Endpoints (Public — no token required)

#### `POST /api/auth/register`
```json
// Request body
{
  "name": "Alice Smith",
  "email": "alice@example.com",
  "password": "securePass123"
}

// Response 201 Created
{
  "success": true,
  "message": "Account created successfully",
  "data": {
    "token": "eyJhbGci...",
    "tokenType": "Bearer",
    "userId": 1,
    "name": "Alice Smith",
    "email": "alice@example.com",
    "role": "VIEWER"
  }
}
```

#### `POST /api/auth/login`
```json
// Request body
{ "email": "alice@example.com", "password": "securePass123" }

// Response 200 OK
{
  "success": true,
  "message": "Login successful",
  "data": { "token": "eyJhbGci...", "role": "VIEWER", ... }
}

// Response 401 Unauthorized
{ "success": false, "message": "Invalid email or password" }
```

---

### Dashboard Endpoints (Bearer token required)

#### `GET /api/viewer/dashboard` — accessible to VIEWER, ANALYST, ADMIN
```json
{
  "success": true,
  "data": {
    "profile": { "id": 1, "name": "Alice", "email": "...", "role": "VIEWER" },
    "currentBalance": 15000.00,
    "recentTransactions": [ ...last 10 records... ]
  }
}
```

#### `GET /api/analyst/dashboard` — accessible to ANALYST, ADMIN
```json
{
  "success": true,
  "data": {
    "profile": { ... },
    "summary": {
      "totalIncome": 50000.00,
      "totalExpenses": 35000.00,
      "netBalance": 15000.00,
      "categoryTotals": { "Rent": 10000.00, "Food": 5000.00 },
      "recentTransactions": [ ... ]
    },
    "monthlyTrends": [
      { "year": 2024, "month": 1, "income": 5000.00, "expenses": 3200.00, "net": 1800.00 },
      { "year": 2024, "month": 2, "income": 5500.00, "expenses": 3800.00, "net": 1700.00 }
    ]
  }
}
```

#### `GET /api/admin/dashboard` — ADMIN only
```json
{
  "success": true,
  "data": {
    "analyticsData": { ...full analyst dashboard data... },
    "totalUsers": 25,
    "activeUsers": 23,
    "totalTransactions": 340
  }
}
```

---

### Transaction Endpoints — ADMIN only

Base path: `/api/admin/transactions`

#### `POST /` — Create transaction
```json
// Request body
{
  "amount": 5000.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2024-01-15",
  "notes": "Monthly salary for January"
}

// Response 201 Created
{ "success": true, "message": "Transaction created successfully", "data": { ...record } }
```

#### `GET /` — Paginated list with optional filters
```
Query parameters (all optional):
  type       → INCOME or EXPENSE
  category   → partial match, case-insensitive ("food" matches "Food", "FOOD")
  startDate  → yyyy-MM-dd inclusive
  endDate    → yyyy-MM-dd inclusive
  page       → zero-indexed page number (default: 0)
  size       → records per page (default: 10, max capped at 100)

Examples:
  GET /api/admin/transactions
  GET /api/admin/transactions?type=EXPENSE
  GET /api/admin/transactions?category=food&page=0&size=20
  GET /api/admin/transactions?startDate=2024-01-01&endDate=2024-03-31
  GET /api/admin/transactions?type=INCOME&category=salary&startDate=2024-01-01
```

Response includes Spring's pagination metadata: `totalElements`, `totalPages`, `number`, `size`.

#### `GET /{id}` — Single transaction
```json
// Response 200 OK
{ "success": true, "data": { "id": 1, "amount": 5000.00, "type": "INCOME", ... } }

// Response 404 Not Found
{ "success": false, "message": "Transaction not found with id: 99" }
```

#### `PUT /{id}` — Partial update (only send fields you want to change)
```json
// Update only amount:
{ "amount": 6000.00 }

// Update only category and notes:
{ "category": "Freelance", "notes": "Updated description" }

// Response 200 OK
{ "success": true, "message": "Transaction updated", "data": { ...updated record } }
```

#### `DELETE /{id}` — Soft delete
```json
// Sets is_deleted = true. Record stays in DB for audit trail.
// Response 200 OK
{ "success": true, "message": "Transaction deleted successfully" }
```

---

### User Management Endpoints — ADMIN only

Base path: `/api/admin/users`

#### `GET /` — List all users
#### `GET /{id}` — Get single user by ID

#### `PATCH /{id}/role` — Change a user's role
```json
// Request body
{ "role": "ANALYST" }

// Business rules enforced:
// - Admin cannot change their own role
// - Cannot demote the last remaining admin
```

#### `PATCH /{id}/status` — Activate or deactivate an account
```json
// Request body
{ "status": "INACTIVE" }

// Business rule: Admin cannot deactivate their own account
```

#### `DELETE /{id}` — Delete a user (hard delete)
```json
// Business rule: Admin cannot delete their own account
// Response 200 OK
{ "success": true, "message": "User deleted successfully" }
```

---

## Data Models

### User Entity — `users` table

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT AUTO_INCREMENT | Primary key |
| `name` | VARCHAR(100) | Display name |
| `email` | VARCHAR(150) UNIQUE | Login identifier |
| `password` | VARCHAR(255) | BCrypt hash — never plain text |
| `role` | VARCHAR(20) | `VIEWER`, `ANALYST`, or `ADMIN` |
| `status` | VARCHAR(20) | `ACTIVE` or `INACTIVE` |
| `created_at` | DATETIME | Auto-set by Hibernate on insert |
| `updated_at` | DATETIME | Auto-set by Hibernate on update |

### Transaction Entity — `transactions` table

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT AUTO_INCREMENT | Primary key |
| `user_id` | BIGINT FK | Who created this record |
| `amount` | DECIMAL(15,2) | Exact decimal — never Float |
| `type` | VARCHAR(10) | `INCOME` or `EXPENSE` |
| `category` | VARCHAR(100) | Free-form (e.g., "Salary", "Rent") |
| `date` | DATE | Business date — when it occurred |
| `notes` | TEXT | Optional description |
| `is_deleted` | BOOLEAN DEFAULT false | Soft delete flag |
| `created_at` | DATETIME | System timestamp — when entered |
| `updated_at` | DATETIME | Auto-set on update |

**Indexes** on `type`, `category`, `date`, and `is_deleted` — these match the most common filter patterns used in dashboard queries.

**Why `DECIMAL` and not `FLOAT` for money?**
Floating-point types cannot represent all decimal values exactly. `0.1 + 0.2` in floating-point does not equal `0.3`. `DECIMAL(15,2)` in MySQL paired with `BigDecimal` in Java gives exact arithmetic throughout the entire stack.

**Why two separate date columns?**
`date` is the business date — when the transaction actually occurred. `created_at` is the system timestamp — when the record was entered. A user entering last month's receipts today will have different values for both. Keeping them separate allows accurate historical reporting.

---

## Dashboard Analytics

All aggregations run at the **database level** — never in Java memory. This is critical for performance as the transactions table grows.

### Total by type
```sql
SELECT COALESCE(SUM(t.amount), 0)
FROM Transaction t
WHERE t.type = :type AND t.isDeleted = false
```
`COALESCE` returns `0.00` instead of `NULL` when there are no rows, preventing NullPointerException in Java.

### Category breakdown
```sql
SELECT t.category, COALESCE(SUM(t.amount), 0)
FROM Transaction t
WHERE t.type = :type AND t.isDeleted = false
GROUP BY t.category
ORDER BY SUM(t.amount) DESC
```
Results are already sorted by largest category first. The service converts raw `Object[]` rows into a `LinkedHashMap` that preserves this order.

### Monthly trends
```sql
SELECT YEAR(t.date), MONTH(t.date), t.type, COALESCE(SUM(t.amount), 0)
FROM Transaction t
WHERE t.isDeleted = false AND t.date >= :fromDate
GROUP BY YEAR(t.date), MONTH(t.date), t.type
ORDER BY YEAR(t.date) ASC, MONTH(t.date) ASC
```
Returns raw rows that the service groups by `(year, month)`. Each month gets one `MonthlyTrendEntry` with income, expenses, and `net = income - expenses` precomputed. The frontend receives ready-to-render data without any additional calculation.

### Dynamic filtering with `Specification<Transaction>`

The transaction list endpoint supports any combination of filters without requiring a separate repository method per combination. `TransactionSpecification.withFilters()` builds the `WHERE` clause at runtime:

```java
// Soft delete is ALWAYS applied — no deleted records ever escape
predicates.add(criteriaBuilder.isFalse(root.get("isDeleted")));

// Each filter is only added if the parameter is non-null
if (type != null)      → AND type = :type
if (category != null)  → AND LOWER(category) LIKE %:category%  (case-insensitive)
if (startDate != null) → AND date >= :startDate
if (endDate != null)   → AND date <= :endDate
```

---

## Error Handling

`GlobalExceptionHandler` catches all exceptions and converts them to a consistent `ApiResponse` shape. The client always receives the same JSON structure regardless of what went wrong.

| Exception | HTTP Status | Trigger |
|-----------|-------------|---------|
| `MethodArgumentNotValidException` | 400 | `@Valid` fails — returns ALL field errors at once |
| `BadRequestException` | 400 | Business logic violation (e.g., self-demotion attempt) |
| `ResourceNotFoundException` | 404 | Entity not found in DB |
| `ConflictException` | 409 | Duplicate resource (e.g., email already registered) |
| `BadCredentialsException` | 401 | Wrong email or password during login |
| `DisabledException` | 401 | Account is `INACTIVE` |
| `AccessDeniedException` | 403 | `@PreAuthorize` check failed — insufficient role |
| `MethodArgumentTypeMismatchException` | 400 | Invalid enum in query params (e.g., `?type=WRONG`) |
| `Exception` (catch-all) | 500 | Any unhandled error — full stack trace logged, generic message to client |

**Validation error response example** — all field errors returned at once, not just the first:
```json
{
  "success": false,
  "message": "Validation failed: {amount: 'must be positive', category: 'required'}",
  "timestamp": "2024-01-15T10:30:00"
}
```

**Security note:** Login failures always return `"Invalid email or password"` — never `"User not found"` or `"Wrong password"`. This prevents user enumeration attacks.

**Internal errors:** The catch-all handler logs the full stack trace server-side but returns only `"An unexpected error occurred"` to the client. Internal implementation details are never exposed.

---

## Validation Rules

### `CreateTransactionRequest`

| Field | Rule |
|-------|------|
| `amount` | Required, positive number, max 13 integer digits, 2 decimal places |
| `type` | Required — `INCOME` or `EXPENSE` |
| `category` | Required, max 100 characters |
| `date` | Required, cannot be a future date |
| `notes` | Optional, max 1000 characters |

### `RegisterRequest`

| Field | Rule |
|-------|------|
| `name` | Required, 2–100 characters |
| `email` | Required, valid email format |
| `password` | Required, minimum 8 characters |

### `UpdateTransactionRequest`

All fields are optional — only provided fields are updated. Validation still applies to any field that is provided (e.g., if amount is sent, it must still be positive).

---

## Setup & Running Locally

### Prerequisites

```
Java 17+
Maven 3.8+
MySQL 8.0+
```

### Step 1 — Clone the repository

```bash
git clone https://github.com/your-username/finance-dashboard-backend.git
cd finance-dashboard-backend
```

### Step 2 — Create the MySQL database

```sql
CREATE DATABASE dashboard;
```

This step is optional if your MySQL user has `CREATE DATABASE` privileges — the connection string includes `createDatabaseIfNotExist=true`.

### Step 3 — Configure `application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/dashboard?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_password_here

jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
jwt.expiration.ms=86400000
```

### Step 4 — Build and run

```bash
mvn clean install
mvn spring-boot:run
```

The API starts at `http://localhost:8080`. Hibernate auto-creates the `users` and `transactions` tables on first startup via `ddl-auto=update`.

### Step 5 — Create the first admin

Register a user via the API:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Admin User","email":"admin@finance.com","password":"admin1234"}'
```

Then promote to admin directly in MySQL:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@finance.com';
```

The `DatabaseSeeder` component also runs at startup and automatically elevates `admin@finance.com` to `ADMIN` if that account already exists with a lower role.

### Step 6 — Use the API

Use the token from the login response as a Bearer token on all subsequent requests:

```bash
curl -H "Authorization: Bearer eyJhbGci..." \
  http://localhost:8080/api/admin/dashboard
```

---

## Environment Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `spring.datasource.url` | MySQL JDBC connection string | localhost:3306/dashboard |
| `spring.datasource.username` | MySQL username | root |
| `spring.datasource.password` | MySQL password | root |
| `jwt.secret` | HMAC-SHA256 signing key (min 32 chars / 256 bits) | See application.properties |
| `jwt.expiration.ms` | Token lifetime in milliseconds | 86400000 (24 hours) |
| `spring.jpa.show-sql` | Log all SQL to console | true |
| `spring.jpa.hibernate.ddl-auto` | Schema strategy | update |

> **Production note:** Set `ddl-auto` to `validate` in production. The `update` setting is convenient for local development but can make unintended schema changes in a live environment.

---

## Design Decisions & Assumptions

**Soft delete for transactions.** Financial records are never physically removed from the database. Setting `is_deleted = true` makes the record invisible to all queries while preserving it for audit and compliance purposes. The `findByIdAndIsDeletedFalse()` repository method ensures soft-deleted records behave as non-existent at the API level — a 404 is returned, not the deleted record.

**DTO layer separation.** Entities are never returned directly from controllers. Every response uses a dedicated DTO class. This prevents accidental exposure of sensitive fields such as the `password` hash on the User entity, decouples the API contract from the database schema, and makes future changes to either layer independent of the other.

**`ApiResponse<T>` envelope.** Every response — success or error — is wrapped in the same shape. The frontend always checks `success`, always reads `message`, always reads `data` if present. Consistent structure eliminates an entire class of frontend parsing errors.

**`BigDecimal` for all money.** `Float` and `Double` introduce rounding errors in decimal arithmetic that are unacceptable in financial systems. `BigDecimal` paired with `DECIMAL(15,2)` in MySQL provides exact representation for all currency values throughout the entire stack.

**Stateless JWT sessions.** No server-side session state is maintained. Every request is independently authenticated using the JWT it carries. This makes the API horizontally scalable — any server instance can handle any request without shared session storage.

**Two-layer RBAC.** URL-level rules in `SecurityConfig` handle coarse routing — is this endpoint accessible to this role category at all? Method-level `@PreAuthorize` handles fine-grained control — can this specific role call this specific method? Separating the two layers makes each independently auditable and keeps the security logic explicit rather than implicit.

**Principle of least privilege on registration.** New users always receive the `VIEWER` role regardless of what they include in the request body. The `ADMIN` role cannot be self-assigned. Role elevation requires explicit action from an existing admin.

**`TransactionSpecification` for dynamic filtering.** Writing one repository method per filter combination would be a combinatorial explosion — separate methods for type only, category only, date only, type plus category, type plus date, and so on. `JpaSpecificationExecutor` lets the service build the `WHERE` clause at runtime from whichever parameters are provided. The soft-delete filter is always included automatically and cannot be bypassed.

**DB-level aggregations.** All dashboard summary queries use SQL `SUM` and `GROUP BY` in JPQL rather than loading all rows into Java and aggregating in memory. For a finance system that may hold millions of transaction records, this architectural choice is the difference between millisecond and minute query times.

**CORS at the Spring Security level.** CORS is configured via the `CorsConfigurationSource` bean registered in `SecurityConfig`, not only via `WebMvcConfigurer`. Spring Security intercepts requests before the MVC layer — a CORS configuration applied only to `WebMvcConfigurer` would not apply to secured endpoints, causing all preflight requests to fail.

---

*Built as a backend assessment demonstrating JWT authentication, multi-tier RBAC, financial data modeling, dashboard analytics, and production-grade error handling in Spring Boot.*
