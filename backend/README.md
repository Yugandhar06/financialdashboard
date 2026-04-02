# Finance Dashboard API

Welcome to the Finance Dashboard API! This is a modern, robust Java backend built with **Spring Boot** and **Spring Security** designed to manage users, track financial transactions (incomes and expenses), and provide comprehensive analytics for a financial dashboard.

## Overview

The application is structured as a REST API and relies on JSON for communication. It features Role-Based Access Control (RBAC) using JWT (JSON Web Tokens) to secure endpoints.

### Core Technologies
- **Java 17+**
- **Spring Boot** (Web, Data JPA, Security)
- **Maven** (Dependency Management)
- **MySQL / H2** (Database)
- **Lombok** (Code reduction for Getters, Setters, Builders)
- **JJWT** (JSON Web Token generation & validation)

## Application Architecture

The project follows a standard Spring Boot layered architecture:
- **Controllers (`com.finance.dashboard.controller`)**: The entry points of the API. Receives HTTP requests, validates input, delegates to the Service layer, and returns standardized JSON responses.
- **Services (`com.finance.dashboard.service`)**: The core business logic. Contains rules for registration, authenticating, and aggregating dashboard analytics.
- **Repositories (`com.finance.dashboard.repository`)**: Interfaces extending Spring Data JPA for abstracting database queries.
- **Entities (`com.finance.dashboard.entity`)**: The object representations of the database tables (e.g., `User`, `Transaction`).
- **DTOs (`com.finance.dashboard.dto`)**: Data Transfer Objects used to receive input (requests) and send output (responses) over the API securely without exposing internal Entity structures.
- **Security (`com.finance.dashboard.security`)**: Holds the filter (`JwtAuthFilter`) responsible for validating JWT tokens on all protected endpoints.

## Features & Endpoints

All responses are wrapped in a standard `ApiResponse` format containing a `success` flag, `message`, and the actual `data`.

### 1. Authentication
Endpoints to register and log in to the application. These endpoints are public.
- `POST /api/auth/register` - Create a new user account.
- `POST /api/auth/login` - Authenticate an existing user and receive a JWT.

### 2. Dashboard Analytics
Provide summary statistics for charting and data visualization. Most of these require `ANALYST` or `ADMIN` roles.
- `GET /api/dashboard/summary` - A complete overview of net balance, recent transactions, and category totals.
- `GET /api/dashboard/category-totals` - Expense breakdowns by category for pie charts.
- `GET /api/dashboard/monthly-trends` - Month-over-month income vs. expense data for line charts.
- `GET /api/dashboard/recent` - Get the last 10 transactions across the entire system.

### 3. Transactions 
Endpoints for CRUD operations on financial records. *(Handled by TransactionController)*

### 4. User Management
Endpoints for admins to manage users (e.g., deactivate accounts or change roles). *(Handled by UserController)*

## Roles & Security
The system uses the following roles:
- **VIEWER**: Can log in and view basic info, but cannot perform deep analysis. New users are assigned this role by default.
- **ANALYST**: Can view comprehensive dashboard analytics and generate reports.
- **ADMIN**: Can manage users, alter permissions, and access everything.

To access protected endpoints, you must send your JWT in the `Authorization` header:
`Authorization: Bearer <your_jwt_token>`

## Getting Started

1. **Install dependencies**: Make sure you have the Lombok Java plugin installed in your IDE (Eclipse/IntelliJ).
2. **Database setup**: Configure your MySQL `root` password in `src/main/resources/application.properties`.
3. **Run the app**: Use `mvnw spring-boot:run` to launch the API on port 8080.
4. **Test**: Use a tool like Postman or cURL to send a POST request to `/api/auth/register` to create your first user!
