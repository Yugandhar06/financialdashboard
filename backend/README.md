# Finance Dashboard API

Welcome to the Finance Dashboard API! This is a modern, enterprise-grade Java backend built using **Spring Boot** and **Spring Security**. It manages users, tracks financial transactions, and provides comprehensive multi-tiered analytics.

Most importantly, this project demonstrates **Clean Architecture** and highly disciplined **Role-Based Access Control (RBAC)** by guaranteeing that each user role operates within strictly defined API boundaries and uniquely tailored data transfer objects.

---

## Technical Stack & Tooling

- **Java 17+**
- **Spring Boot 3.x** (Web, Data JPA, Security)
- **Spring Security & JJWT** (Stateless JSON Web Tokens for Authentication)
- **MySQL / H2** (Relational Database)
- **Maven** (Dependency Management)
- **Lombok** (Boilerplate reduction for Getters/Setters/Builders)

---

## Architectural Highlights

The system leverages standard Spring Boot layered architecture (`Controllers → Services → Repositories`) but enforces structural segregation for its roles to ensure zero data leakage.

### 1. Hardened URL-Level Role Security

The `SecurityConfig` is configured to intercept HTTP requests based on URL path matchers. This offers a highly visible, top-level guarantee of security:

- `/api/viewer/**` — Accessible by `VIEWER`, `ANALYST`, and `ADMIN`.
- `/api/analyst/**` — Accessible by `ANALYST` and `ADMIN`.
- `/api/admin/**` — Strictly accessible by `ADMIN` only.

### 2. Tiered DTO Hierarchy 

The API uses Data Transfer Objects (DTOs) heavily, but specific roles receive specifically tailored subsets of data. The system embeds responses cleanly:
*   `ViewerDashboardResponse` -> Contains only basic profile, balance, and recent global transactions.
*   `AnalystDashboardResponse` -> Embeds the Viewer profile, but adds massive category breakdowns, monthly trends, and high-level summaries.
*   `AdminDashboardResponse` -> Embeds the Analyst data, but adds full system administrative metrics (total active users, global transaction counts).

---

## Endpoints Summary

All responses are wrapped in a standard `ApiResponse<T>` wrapper class containing a `success` flag, a string `message`, and the requested JSON payload `data`.

### 🔓 Authentication Endpoints (Public)
Endpoints to register and log in to the application.
- `POST /api/auth/register` - Create a new user account.
- `POST /api/auth/login` - Authenticate an existing user and receive a valid JWT token.

### 👤 Viewer Dashboard (`/api/viewer`)
*Requires VIEWER role or higher.*
- `GET /api/viewer/dashboard` — Retrieves `ViewerDashboardResponse`.

### 📊 Analyst Dashboard (`/api/analyst`)
*Requires ANALYST role or higher.*
- `GET /api/analyst/dashboard` — Retrieves `AnalystDashboardResponse`, executing deep SQL-level grouped aggregations across all transactions.

### 🛠️ Admin Ecosystem (`/api/admin`)
*Strictly ADMIN role.*
- `GET /api/admin/dashboard` — Retrieves `AdminDashboardResponse` including active system metrics.
- `GET /api/admin/users` — Fetch all registered users for administration.
- `PATCH /api/admin/users/{id}/role` — Delegate or demote a user's role.
- `POST /api/admin/transactions` — Submit back-dated or manual financial ledger entries.
- `DELETE /api/admin/transactions/{id}` — Perform system audits and soft-delete financial records.

---

## Database Optimization Note

All financial aggregations (e.g., finding the net balance of 10,000 transactions, or sorting monthly trends) happens **at the database level** (`transactionRepository.getMonthlyTrends()`).
- We avoid pulling objects into Java memory for summing loops.
- Core math operations use `BigDecimal` to ensure absolute financial ledger accuracy and circumvent floating point errors natively found in floats/doubles. 

---

## Getting Started

1. **Install dependencies**: Make sure you have the Lombok Java plugin installed in your IDE (Eclipse/IntelliJ).
2. **Database setup**: Configure your MySQL `root` connections in `src/main/resources/application.properties`.
3. **Run the app**: Use `mvnw spring-boot:run` to launch the API locally on port 8080.
4. **Test**: Use a tool like Postman to POST to `/api/auth/register` to generate an Admin user, login to retrieve the Bearer `<Token>`, and access `/api/admin/dashboard`!
