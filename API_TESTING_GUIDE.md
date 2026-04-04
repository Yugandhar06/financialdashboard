# API Testing Guide

## Manual Testing with cURL / PowerShell

This guide provides step-by-step examples to test all major endpoints of the financial dashboard backend.

### Prerequisites

- Backend running on `http://localhost:8080`
- MySQL database configured
- Valid JWT tokens obtained from login

---

## 1. Authentication Flow

### Step 1.1: Register New User

```powershell
$headers = @{'Content-Type' = 'application/json'}
$body = @{
    name = 'Test User'
    email = 'testuser@finance.com'
    password = 'Password123!'
} | ConvertTo-Json

$response = Invoke-WebRequest -Uri 'http://localhost:8080/api/auth/register' `
    -Method Post `
    -Headers $headers `
    -Body $body `
    -UseBasicParsing

$result = $response.Content | ConvertFrom-Json
$result | ConvertTo-Json -Depth 3
```

**Expected Response (201 Created):**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "userId": 1,
    "name": "Test User",
    "email": "testuser@finance.com",
    "role": "VIEWER"  // Default role
  }
}
```

### Step 1.2: Login Existing User

```powershell
$headers = @{'Content-Type' = 'application/json'}
$body = @{
    email = 'testuser@finance.com'
    password = 'Password123!'
} | ConvertTo-Json

$response = Invoke-WebRequest -Uri 'http://localhost:8080/api/auth/login' `
    -Method Post `
    -Headers $headers `
    -Body $body `
    -UseBasicParsing

$result = $response.Content | ConvertFrom-Json
$token = $result.data.token
$result | ConvertTo-Json -Depth 3
```

**Note:** Save the `token` value for subsequent requests.

---

## 2. Dashboard Access (All Authenticated Users)

### Step 2.1: View Viewer Dashboard (VIEWER can access)

```powershell
$token = 'eyJhbGciOiJIUzI1NiJ9...'  # Replace with actual token
$headers = @{'Authorization' = "Bearer $token"}

$response = Invoke-WebRequest -Uri 'http://localhost:8080/api/viewer/dashboard' `
    -Method Get `
    -Headers $headers `
    -UseBasicParsing

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 5
```

**Expected Response (200 OK):**
```json
{
  "success": true,
  "message": "Viewer dashboard retrieved",
  "data": {
    "profile": {
      "id": 1,
      "name": "Test User",
      "email": "testuser@finance.com",
      "role": "VIEWER",
      "status": "ACTIVE"
    },
    "currentBalance": 43725397.00,
    "recentTransactions": [
      {
        "id": 1,
        "amount": 5000.00,
        "type": "INCOME",
        "category": "Salary",
        "date": "2026-01-15"
      }
    ]
  }
}
```

---

## 3. Testing Role-Based Access Control

### Test 3.1: VIEWER Tries to Create Transaction (Should Fail)

```powershell
$token = 'eyJhbGciOiJIUzI1NiJ9...'  # VIEWER token
$headers = @{
    'Authorization' = "Bearer $token"
    'Content-Type' = 'application/json'
}
$body = @{
    amount = 1000.00
    type = 'INCOME'
    category = 'Salary'
    date = '2026-02-01'
    notes = 'Test'
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/admin/transactions' `
        -Method Post `
        -Headers $headers `
        -Body $body `
        -UseBasicParsing
} catch {
    Write-Host "Status Code: $($_.Exception.Response.StatusCode)"  # Should be 403
    Write-Host "Description: $($_.Exception.Response.StatusDescription)"
}
```

**Expected Response (403 Forbidden):**
- VIEWER cannot access `/api/admin/` endpoints
- System enforces role-based restrictions

---

## 4. Transaction Management (Admin Only)

### Step 4.1: Get Admin Token

First, you need to either:
1. Register/login as a user and promote them to ADMIN (requires existing ADMIN)
2. Create a test admin account directly in MySQL:

```sql
INSERT INTO users (name, email, password, role, status) VALUES (
    'Test Admin',
    'admin@test.com',
    '$2a$10$GENERATED_BCRYPT_HASH',  -- Use BCrypt generator
    'ADMIN',
    'ACTIVE'
);
```

### Step 4.2: Create Transaction (Admin Only)

```powershell
$token = 'ADMIN_TOKEN_HERE'
$headers = @{
    'Authorization' = "Bearer $token"
    'Content-Type' = 'application/json'
}
$body = @{
    amount = 5000.00
    type = 'INCOME'
    category = 'Monthly Salary'
    date = '2026-01-15'
    notes = 'January salary'
} | ConvertTo-Json

$response = Invoke-WebRequest -Uri 'http://localhost:8080/api/admin/transactions' `
    -Method Post `
    -Headers $headers `
    -Body $body `
    -UseBasicParsing

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 5
```

**Expected Response (201 Created):**
```json
{
  "success": true,
  "message": "Transaction created successfully",
  "data": {
    "id": 1,
    "amount": 5000.00,
    "type": "INCOME",
    "category": "Monthly Salary",
    "date": "2026-01-15",
    "notes": "January salary",
    "createdAt": "2026-04-04T21:50:49.007807"
  }
}
```

### Step 4.3: List Transactions (Paginated & Filtered)

```powershell
# Get all transactions
$response = Invoke-WebRequest `
    -Uri 'http://localhost:8080/api/admin/transactions' `
    -Method Get `
    -Headers @{'Authorization' = "Bearer $token"} `
    -UseBasicParsing

# Get with filters
$response = Invoke-WebRequest `
    -Uri 'http://localhost:8080/api/admin/transactions?type=INCOME&category=Salary&page=0&size=10' `
    -Method Get `
    -Headers @{'Authorization' = "Bearer $token"} `
    -UseBasicParsing

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 5
```

**Query Parameters:**
- `type` — INCOME or EXPENSE
- `category` — Free-text search (case-insensitive)
- `startDate` — Format: yyyy-MM-dd
- `endDate` — Format: yyyy-MM-dd
- `page` — Zero-indexed (default: 0)
- `size` — Records per page (default: 10, max: 100)

### Step 4.4: Get Single Transaction

```powershell
$transactionId = 1
$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/api/admin/transactions/$transactionId" `
    -Method Get `
    -Headers @{'Authorization' = "Bearer $token"} `
    -UseBasicParsing

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 5
```

### Step 4.5: Update Transaction (Partial Update)

```powershell
$transactionId = 1
$headers = @{
    'Authorization' = "Bearer $token"
    'Content-Type' = 'application/json'
}
$body = @{
    amount = 6000.00  # Only update amount, keep others same
} | ConvertTo-Json

$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/api/admin/transactions/$transactionId" `
    -Method Put `
    -Headers $headers `
    -Body $body `
    -UseBasicParsing

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 5
```

### Step 4.6: Delete Transaction (Soft Delete)

```powershell
$transactionId = 1
$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/api/admin/transactions/$transactionId" `
    -Method Delete `
    -Headers @{'Authorization' = "Bearer $token"} `
    -UseBasicParsing

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 5
```

---

## 5. User Management (Admin Only)

### Step 5.1: List All Users

```powershell
$response = Invoke-WebRequest `
    -Uri 'http://localhost:8080/api/users' `
    -Method Get `
    -Headers @{'Authorization' = "Bearer $adminToken"} `
    -UseBasicParsing

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 5
```

### Step 5.2: Change User Role

```powershell
$userId = 1
$headers = @{
    'Authorization' = "Bearer $adminToken"
    'Content-Type' = 'application/json'
}
$body = @{
    role = 'ANALYST'  # Promote VIEWER → ANALYST
} | ConvertTo-Json

$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/api/users/$userId/role" `
    -Method Patch `
    -Headers $headers `
    -Body $body `
    -UseBasicParsing

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 5
```

### Step 5.3: Deactivate User

```powershell
$userId = 2
$headers = @{
    'Authorization' = "Bearer $adminToken"
    'Content-Type' = 'application/json'
}
$body = @{
    status = 'INACTIVE'
} | ConvertTo-Json

$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/api/users/$userId/status" `
    -Method Patch `
    -Headers $headers `
    -Body $body `
    -UseBasicParsing

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 5
```

---

## 6. Error Testing

### Test 6.1: Invalid JWT Token

```powershell
$invalidToken = 'invalid.token.here'
$response = Invoke-WebRequest `
    -Uri 'http://localhost:8080/api/viewer/dashboard' `
    -Method Get `
    -Headers @{'Authorization' = "Bearer $invalidToken"} `
    -UseBasicParsing

# Expected: 403 Forbidden
```

### Test 6.2: Missing Required Fields

```powershell
$headers = @{
    'Authorization' = "Bearer $adminToken"
    'Content-Type' = 'application/json'
}
$body = @{
    amount = 1000
    # Missing: type, category, date
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest `
        -Uri 'http://localhost:8080/api/admin/transactions' `
        -Method Post `
        -Headers $headers `
        -Body $body `
        -UseBasicParsing
} catch {
    $_.Exception.Response.StatusCode  # Expected: 400
    $_.Exception.Response | ConvertTo-Json -Depth 3
}
```

### Test 6.3: Invalid Amount (Negative)

```powershell
$headers = @{
    'Authorization' = "Bearer $adminToken"
    'Content-Type' = 'application/json'
}
$body = @{
    amount = -100
    type = 'INCOME'
    category = 'Invalid'
    date = '2026-01-01'
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest `
        -Uri 'http://localhost:8080/api/admin/transactions' `
        -Method Post `
        -Headers $headers `
        -Body $body `
        -UseBasicParsing
} catch {
    $_.Exception.Response.StatusCode  # Expected: 400
}
```

### Test 6.4: Duplicate Email Registration

```powershell
$headers = @{'Content-Type' = 'application/json'}
$body = @{
    name = 'User One'
    email = 'same@email.com'
    password = 'Password123!'
} | ConvertTo-Json

# First registration - succeeds
Invoke-WebRequest -Uri 'http://localhost:8080/api/auth/register' `
    -Method Post -Headers $headers -Body $body -UseBasicParsing

# Second registration with same email - fails with 409 Conflict
try {
    Invoke-WebRequest -Uri 'http://localhost:8080/api/auth/register' `
        -Method Post -Headers $headers -Body $body -UseBasicParsing
} catch {
    $_.Exception.Response.StatusCode  # Expected: 409 Conflict
}
```

---

## 7. Dashboard Analytics (Role-Specific)

### Test 7.1: Analyst Dashboard

```powershell
# After promoting user to ANALYST role
$headers = @{'Authorization' = "Bearer $analystToken"}
$response = Invoke-WebRequest `
    -Uri 'http://localhost:8080/api/analyst/dashboard' `
    -Method Get `
    -Headers $headers `
    -UseBasicParsing

$result = $response.Content | ConvertFrom-Json
$result.data.summary  # Shows income, expenses, balance, category totals
$result.data.monthlyTrends  # Shows 12-month trend data
```

### Test 7.2: Admin Dashboard

```powershell
# Admin only
$headers = @{'Authorization' = "Bearer $adminToken"}
$response = Invoke-WebRequest `
    -Uri 'http://localhost:8080/api/admin/dashboard' `
    -Method Get `
    -Headers $headers `
    -UseBasicParsing

$result = $response.Content | ConvertFrom-Json
$result.data.totalUsers  # System-wide user count
$result.data.totalTransactions  # System-wide transaction count
$result.data.analyticsData  # All analyst data included
```

---

## Complete Test Sequence

Here's a PowerShell script that performs a complete testing sequence:

```powershell
param(
    [string]$BaseUrl = 'http://localhost:8080'
)

Write-Host "=== Financial Dashboard API Testing ===" -ForegroundColor Cyan

# 1. Register User
Write-Host "`n1. Registering user..." -ForegroundColor Yellow
$regBody = @{
    name = "Test User $(Get-Random)"
    email = "test_$(Get-Random)@finance.com"
    password = "Test@123456"
} | ConvertTo-Json

$reg = Invoke-WebRequest -Uri "$BaseUrl/api/auth/register" `
    -Method Post `
    -Headers @{'Content-Type' = 'application/json'} `
    -Body $regBody `
    -UseBasicParsing

$regData = ($reg.Content | ConvertFrom-Json).data
$userEmail = $regData.email
$userId = $regData.userId
Write-Host "✓ User registered: $userEmail (ID: $userId)" -ForegroundColor Green

# 2. Login
Write-Host "`n2. Logging in..." -ForegroundColor Yellow
$loginBody = @{
    email = $userEmail
    password = "Test@123456"
} | ConvertTo-Json

$login = Invoke-WebRequest -Uri "$BaseUrl/api/auth/login" `
    -Method Post `
    -Headers @{'Content-Type' = 'application/json'} `
    -Body $loginBody `
    -UseBasicParsing

$token = ($login.Content | ConvertFrom-Json).data.token
$role = ($login.Content | ConvertFrom-Json).data.role
Write-Host "✓ Login successful. Role: $role" -ForegroundColor Green

# 3. Access Viewer Dashboard
Write-Host "`n3. Accessing viewer dashboard..." -ForegroundColor Yellow
$vdash = Invoke-WebRequest -Uri "$BaseUrl/api/viewer/dashboard" `
    -Method Get `
    -Headers @{'Authorization' = "Bearer $token"} `
    -UseBasicParsing

$balance = ($vdash.Content | ConvertFrom-Json).data.currentBalance
Write-Host "✓ Dashboard accessed. Current balance: $balance" -ForegroundColor Green

# 4. Test Access Control (Try to create transaction as VIEWER - should fail)
Write-Host "`n4. Testing access control (VIEWER trying to POST)..." -ForegroundColor Yellow
$txBody = @{
    amount = 1000
    type = 'INCOME'
    category = 'Test'
    date = '2026-01-01'
} | ConvertTo-Json

try {
    Invoke-WebRequest -Uri "$BaseUrl/api/admin/transactions" `
        -Method Post `
        -Headers @{
            'Authorization' = "Bearer $token"
            'Content-Type' = 'application/json'
        } `
        -Body $txBody `
        -UseBasicParsing
    Write-Host "✗ ERROR: VIEWER was able to POST (should fail)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 403) {
        Write-Host "✓ Access correctly denied (403 Forbidden)" -ForegroundColor Green
    } else {
        Write-Host "✗ Wrong error code: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
    }
}

Write-Host "`n=== Testing Complete ===" -ForegroundColor Cyan
```

---

## Summary of Key Test Scenarios

| Scenario | Expected Result | HTTP Code |
|----------|-----------------|-----------|
| Register new user | Returns JWT token | 201 |
| Login valid credentials | Returns JWT token | 200 |
| Login invalid credentials | Error response | 401 |
| VIEWER accesses viewer dashboard | Success | 200 |
| VIEWER creates transaction | Forbidden | 403 |
| ADMIN creates transaction | Success | 201 |
| ADMIN lists transactions | Paginated results | 200 |
| Access with invalid token | Forbidden | 403 |
| Missing required fields | Bad request | 400 |
| Negative amount | Bad request | 400 |
| Duplicate email | Conflict | 409 |
| Update single transaction | Updated object | 200 |
| Delete transaction | Success message | 200 |

