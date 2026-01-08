# Budget Tracker API Documentation

This document provides comprehensive documentation for all REST API endpoints in the Budget Tracker application.

## Project Information

- Controllers Location: project/src/main/java/com/example/budgettracker/controller/
- Base URL: http://localhost:8080
- Framework: Spring Boot
- Authentication: Google OAuth 2.0 and Session-based Bearer Token Authentication
- Email Service: SMTP (for password reset functionality)
- Generated: 2025

---

## Authentication APIs (/api/auth)

### 1. POST /api/auth/login
- Description: Authenticate user with email and password
- Requires: Request body with LoginRequest (email, password)
- Returns: AuthResponse containing sessionToken, userId, email, and message

### 2. POST /api/auth/signup
- Description: Register a new user account
- Requires: Request body with SignupRequest (email, password, firstName, lastName)
- Returns: AuthResponse containing sessionToken, userId, email, and message

### 3. POST /api/auth/login/idp
- Description: Authenticate user via Identity Provider (Google OAuth 2.0)
- Requires: Request body with IdpLoginRequest (provider, idToken, email)
- Returns: AuthResponse containing sessionToken, userId, email, and message

### 4. POST /api/auth/logout
- Description: Logout current user and invalidate session
- Requires: Authorization header with Bearer token
- Returns: 200 OK with empty body

### 5. GET /api/auth/me
- Description: Get current authenticated user details
- Requires: Authorization header with Bearer token
- Returns: User object containing userId, email, firstName, lastName

### 6. GET /api/auth/validate
- Description: Validate current session token
- Requires: Authorization header with Bearer token
- Returns: Boolean indicating if session is valid

---

## Password Reset APIs (/api/auth)

### 1. POST /api/auth/forgot-password
- Description: Request password reset email (15-minute token expiry)
- Requires: Request body with email address
- Returns: PasswordResetResponse with success status and message
- Note: Always returns success to prevent email enumeration attacks

### 2. GET /api/auth/reset-password/{token}
- Description: Validate password reset token before showing reset form
- Requires: Path variable: token (password reset token)
- Returns: PasswordResetResponse with validation status

### 3. POST /api/auth/reset-password
- Description: Reset password using valid token
- Requires: Request body with token and newPassword
- Returns: PasswordResetResponse with success status and message

### 4. POST /api/auth/validate-password
- Description: Validate password strength for frontend validation
- Requires: Request body with password field
- Returns: PasswordResetResponse with validation status

---

## Session Management APIs (/api/session)

### 1. GET /api/session/validate
- Description: Validate current session status and check if token is still active
- Requires: Authorization header with Bearer token
- Returns: JSON object with "valid" boolean and reason/message

### 2. POST /api/session/renew
- Description: Manually renew session (heartbeat endpoint) to extend session expiration
- Requires: Authorization header with Bearer token
- Returns: JSON object with success status and message, or HTTP 401 if renewal fails

### 3. GET /api/session/status
- Description: Get detailed session status and information including issued time, expiration, and last activity
- Requires: Authorization header with Bearer token
- Returns: Session details (issuedAt, expiresAt, lastActivity, ipAddress), or HTTP 401 if invalid

### 4. POST /api/session/cleanup
- Description: Administrative endpoint to cleanup expired sessions from the system
- Requires: None (administrative endpoint)
- Returns: Success/failure message indicating cleanup completion status

---

## Income APIs (/api/income)

### 1. POST /api/income
- Description: Record a new income transaction for the authenticated user
- Requires: Authorization header, IncomeRequest body (validated)
- Returns: IncomeResponse with transaction details

### 2. GET /api/income
- Description: Retrieve all income transactions for the authenticated user
- Requires: Authorization header
- Returns: List of IncomeResponse objects

### 3. GET /api/income/default-account
- Description: Get or create the default account for the user. If no account exists, creates a new "Cash Account" with zero balance
- Requires: Authorization header
- Returns: Account details (accountId, accountName, balance)

---

## Expense APIs (/api/expenses)

### 1. POST /api/expenses
- Description: Record a new expense transaction. Includes error handling for authentication, insufficient balance, and category validation
- Requires: Authorization header, ExpenseRequest body (validated)
- Returns: ExpenseResponse with transaction details or error message

### 2. GET /api/expenses
- Description: Retrieve all expense transactions for the authenticated user
- Requires: Authorization header
- Returns: List of ExpenseResponse objects

---

## Category APIs (/api/categories)

### 1. GET /api/categories
- Description: List all categories for the authenticated user (includes system and custom categories)
- Requires: Authorization header OR X-User-Id header
- Returns: List of CategoryResponse objects

### 2. POST /api/categories
- Description: Create a new custom category for the user
- Requires: Authorization header OR X-User-Id header, CategoryRequest body (validated)
- Returns: CategoryResponse with HTTP 200 OK

### 3. PUT /api/categories/{id}
- Description: Rename an existing category
- Requires: Authorization header OR X-User-Id header, category ID path variable, CategoryRequest body
- Returns: CategoryResponse with updated category details

### 4. DELETE /api/categories/{id}
- Description: Delete a category and optionally reassign associated transactions to another category
- Requires: Authorization header OR X-User-Id header, category ID path variable
- Optional param: reassignTo (Long) - category ID to reassign transactions to
- Returns: HTTP 204 No Content

### 5. POST /api/categories/merge
- Description: Merge multiple categories into one, reassigning all transactions
- Requires: Authorization header OR X-User-Id header, CategoryMergeRequest body
- Returns: JSON object with count of reassigned transactions

### 6. GET /api/categories/{id}/summary
- Description: Get financial summary for a specific category including total income and expenses
- Requires: Authorization header OR X-User-Id header, category ID path variable
- Optional param: month (format: yyyy-MM)
- Returns: CategorySummaryResponse with aggregated financial data

### 7. GET /api/categories/{id}/records
- Description: List all transactions (income/expense records) for a specific category
- Requires: Authorization header OR X-User-Id header, category ID path variable
- Optional params: month (yyyy-MM), type (Income/Expense)
- Returns: List of CategoryRecordResponse objects

---

## Goal APIs (/api/goals)

### 1. POST /api/goals
- Description: Create a new savings goal for the user
- Requires: Authorization header, GoalRequest body
- Returns: GoalResponse with HTTP 201 Created status and location header

### 2. GET /api/goals
- Description: Get all goals for the authenticated user. Supports both token-based and session-based authentication (JSESSIONID)
- Requires: Authorization header (optional if session exists)
- Returns: List of GoalResponse objects

### 3. GET /api/goals/{id}
- Description: Retrieve a specific goal by ID for the authenticated user
- Requires: Authorization header, goal ID path variable
- Returns: GoalResponse for the specified goal

### 4. PATCH /api/goals/{id}
- Description: Partially update an existing goal (e.g., change target amount, name, or deadline)
- Requires: Authorization header, goal ID path variable, GoalRequest body
- Returns: Updated GoalResponse

### 5. DELETE /api/goals/{id}
- Description: Delete a specific goal
- Requires: Authorization header, goal ID path variable
- Returns: HTTP 204 No Content

### 6. POST /api/goals/contribute
- Description: Make a contribution towards a specific goal
- Requires: Authorization header, ContributionRequest body
- Returns: ContributionResponse with contribution details

### 7. GET /api/goals/{id}/contributions
- Description: Get all contributions for a specific goal, optionally filtered by date range
- Requires: Authorization header, goal ID path variable
- Optional params: from (LocalDate), to (LocalDate)
- Returns: List of CashFlow objects representing contributions

### 8. GET /api/goals/debug/session-info
- Description: Debug endpoint to view session and security context information (should be removed before production)
- Returns: Session details, attributes, and security context

---

## Budget APIs (/api/categories/{categoryId}/budget)

### 1. PUT /api/categories/{categoryId}/budget
- Description: Set or update the budget amount for a specific category and month
- Requires: Authorization header, categoryId path variable, BudgetUpdateRequest body (budget amount, yearMonth)
- Returns: BudgetSummaryResponse with updated budget details

### 2. GET /api/categories/{categoryId}/budget/summary
- Description: Get budget summary for a specific category and month, including budget amount and spending totals
- Requires: Authorization header, categoryId path variable
- Optional param: yearMonth (format: yyyy-MM)
- Returns: BudgetSummaryResponse with budget vs actual spending

---

## Simple Budget APIs (/api/budget)

### 1. POST /api/budget/set
- Description: Set monthly category budget for the current month
- Requires: Authorization header, BudgetSetRequest body (categoryId, amount, optional customName)
- Returns: BudgetSummaryResponse with budget details

### 2. GET /api/budget/categories
- Description: Get all available categories for budget setting
- Requires: None
- Returns: List of category objects with categoryId and name

### 3. GET /api/budget/summary/{categoryId}
- Description: Get budget summary for a specific category for the current month
- Requires: Authorization header, categoryId path variable
- Returns: BudgetSummaryResponse with budget vs spending details

### 4. DELETE /api/budget/delete/{categoryId}
- Description: Delete budget for a specific category for the current month
- Requires: Authorization header, categoryId path variable
- Returns: Success/error message with categoryId

---

## Dashboard APIs (/api/dashboard)

### 1. GET /api/dashboard
- Description: Get complete dashboard data including financial overview, recent transactions, and summary statistics for the authenticated user
- Requires: Authorization header
- Returns: DashboardResponse with all dashboard data or HTTP 503 if data unavailable

### 2. GET /api/dashboard/financial-aggregates
- Description: Get only financial aggregates (total income, expenses, balance, etc.) without full dashboard data
- Requires: Authorization header
- Returns: FinancialAggregatesResponse with summary financial data

### 3. GET /api/dashboard/spending-trend
- Description: Get spending trend data for charts over a specified time period (week, month, or year)
- Requires: Authorization header
- Optional param: period (default: "WEEK", options: "WEEK", "MONTH", "YEAR")
- Returns: SpendingTrendData with time-series spending information

---

## BudgetCoin APIs (/api/budgetcoin)

### 1. GET /api/budgetcoin/balance
- Description: Retrieve the current BudgetCoin balance for a user
- Requires: userId query parameter
- Returns: RewardBalanceResponse containing userId and balance

### 2. POST /api/budgetcoin/grant
- Description: Grant BudgetCoin to a user. Automatically generates a rewardEventId if not provided
- Requires: RewardGrantRequest body (validated)
- Returns: RewardGrantResponse with grant details

### 3. POST /api/budgetcoin/redeem
- Description: Redeem BudgetCoin for a user
- Requires: RewardRedeemRequest body (validated)
- Returns: RewardRedeemResponse with redemption details

### 4. GET /api/budgetcoin/grants
- Description: Retrieve all BudgetCoin grant records for a user
- Requires: userId query parameter
- Returns: List of RewardGrantResponse objects

### 5. GET /api/budgetcoin/redeems
- Description: Retrieve all BudgetCoin redemption records for a user
- Requires: userId query parameter
- Returns: List of RewardRedeemResponse objects

---

## Report APIs (/api/reports)

### 1. GET /api/reports/data
- Description: Retrieve summarized financial report data (income, expense, balance, and expense breakdown) for a given user and date range
- Requires: userId, startDate, endDate as query parameters (ISO format: yyyy-MM-dd)
- Returns: JSON object containing userId, startDate, endDate, and totals map with financial metrics

### 2. GET /api/reports/chart
- Description: Generate and return a PNG image of the financial report, including a pie chart of expenses and textual summary
- Requires: userId, startDate, endDate as query parameters (ISO format: yyyy-MM-dd)
- Returns: PNG image (MediaType.IMAGE_PNG) with chart and annotated financial summary

---

## Item APIs (/api/item)

### 1. GET /api/item/all
- Description: Retrieve all available items in the system
- Requires: None
- Returns: List of ItemResponse objects

---

## Static/Redirect Endpoints (/)

### 1. GET /favicon.ico
- Description: Handle favicon requests without throwing errors
- Returns: HTTP 204 No Content

### 2. GET /
- Description: Redirect to login page
- Returns: Redirect to /html/login.html

### 3. GET /index.html
- Description: Redirect to login page
- Returns: Redirect to /html/login.html

### 4. GET /login.html
- Description: Redirect to login page in html directory
- Returns: Redirect to /html/login.html

### 5. GET /signup.html
- Description: Redirect to signup page in html directory
- Returns: Redirect to /html/signup.html

### 6. GET /oversave-dashboard.html
- Description: Redirect to dashboard page in html directory
- Returns: Redirect to /html/oversave-dashboard.html

### 7. GET /transactions_page.html
- Description: Redirect to transactions page in html directory
- Returns: Redirect to /html/transactions_page.html

### 8. GET /budgets_page.html
- Description: Redirect to budgets page in html directory
- Returns: Redirect to /html/budgets_page.html

### 9. GET /goals_page.html
- Description: Redirect to goals page in html directory
- Returns: Redirect to /html/goals_page.html

### 10. GET /analytics_page.html
- Description: Redirect to analytics page in html directory
- Returns: Redirect to /html/analytics_page.html

### 11. GET /subscriptions_page.html
- Description: Redirect to subscriptions page in html directory
- Returns: Redirect to /html/subscriptions_page.html

### 12. GET /calendar_page.html
- Description: Redirect to calendar page in html directory
- Returns: Redirect to /html/calendar_page.html

### 13. GET /shopping_page.html
- Description: Redirect to shopping page in html directory
- Returns: Redirect to /html/shopping_page.html

---

## Authentication & Authorization Notes

### Session-based Authentication
- Most endpoints require Authorization: Bearer {sessionToken} header
- Session tokens are obtained from login/signup endpoints
- Tokens are validated on each request via BaseController.getUserIdFromToken()

### Google OAuth 2.0
- Use /api/auth/login/idp endpoint with Google idToken
- Provider field should be set to identify OAuth provider
- Returns same AuthResponse as regular login

### SMTP Email
- Used for password reset functionality
- Emails sent with 15-minute expiry tokens
- Always returns success to prevent email enumeration attacks

### Public Endpoints
- /api/item/all - No authentication required
- /api/reports/data and /api/reports/chart - Uses userId query parameter (should be secured in production)

### Alternative Authentication
Some endpoints (like Category APIs) support alternative authentication via X-User-Id header:
```
X-User-Id: {userId}
```

### Error Responses
Controllers typically return error responses with HTTP status codes:
- 400 Bad Request: Invalid input or business logic error
- 401 Unauthorized: Missing or invalid authentication
- 404 Not Found: Resource not found
- 500 Internal Server Error: Server-side error
- 503 Service Unavailable: Service temporarily unavailable

### Date Formats
- ISO Date format: yyyy-MM-dd (e.g., 2025-01-15)
- Year-Month format: yyyy-MM (e.g., 2025-01)

---

## Common Request/Response DTOs

### AuthResponse
```json
{
  "sessionToken": "string",
  "userId": "long",
  "email": "string",
  "message": "string"
}
```

### ExpenseRequest
```json
{
  "amount": "BigDecimal",
  "categoryId": "long",
  "accountId": "long",
  "description": "string",
  "date": "LocalDate"
}
```

### GoalRequest
```json
{
  "title": "string",
  "targetAmount": "BigDecimal",
  "targetDate": "LocalDate",
  "description": "string"
}
```

### BudgetSetRequest
```json
{
  "categoryId": "long",
  "amount": "BigDecimal",
  "customName": "string (optional)"
}
```

---

## Notes

1. All endpoints that require authorization will return HTTP 401 if the session token is missing or invalid
2. Validation errors will return HTTP 400 with details about the validation failure
3. The BaseController provides common authentication utilities used by most controllers
4. Session management follows the FR-6 functional requirement for automatic session renewal
5. Debug endpoints (e.g., /api/goals/debug/session-info) should be removed before production deployment
6. Password reset tokens expire after 15 minutes for security
7. Public endpoints should be reviewed and secured before production deployment
8. Google OAuth 2.0 integration provides alternative authentication method
9. SMTP email service is configured for password reset functionality
10. Session tokens are validated on each authenticated request