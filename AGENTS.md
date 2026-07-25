# AGENTS.md

## Project Identity

Project name: NexaERP

Repository structure:

```text
Project_Debug/
├── backend/
├── nexa-erp-frontend/
└── AGENTS.md
```

NexaERP is a personal portfolio Finance ERP project.

It is intended to demonstrate professional Java full-stack development skills for GitHub, interviews and job applications.

It is not a client production system.

The goal is to keep the system:

* Technically correct
* Easy to understand
* Easy to demonstrate
* Well structured
* Visually professional
* Free from unnecessary enterprise complexity

---

# Technology Stack

## Backend

* Java 21
* Spring Boot
* Spring Security
* JWT authentication
* Refresh tokens
* Spring Data JPA
* Hibernate
* MySQL
* Maven
* Lombok
* REST APIs

Backend location:

```text
backend/
```

Backend base package:

```text
com.nexaerp
```

Default backend port:

```text
8085
```

## Frontend

* Angular 21
* Standalone components
* TypeScript
* Angular Signals
* Reactive Forms
* Bootstrap 5
* Bootstrap Icons
* SCSS
* Angular Router

Frontend location:

```text
nexa-erp-frontend/
```

Default frontend port:

```text
4200
```

---

# General Instructions

Always inspect the existing implementation before making changes.

Do not guess project structure, API names, DTO fields, routes, permissions, entity relationships or business rules.

Use the existing architecture and naming conventions.

Do not rewrite working modules without a clear technical reason.

Do not make unrelated changes.

Do not modify multiple major features in one task.

Complete only the feature explicitly requested by the user.

After completing a feature, stop and report the result.

Before modifying files:

1. Inspect relevant backend files.
2. Inspect relevant frontend files.
3. Identify existing APIs and DTOs.
4. Identify permission requirements.
5. Identify business rules.
6. Check whether reusable code already exists.
7. Explain the planned changes briefly.

After modifying files:

1. Run backend compile.
2. Run relevant backend tests.
3. Run Angular production build.
4. Run relevant frontend tests if available.
5. Report exact files created.
6. Report exact files modified.
7. Report database changes.
8. Report API changes.
9. Report manual test steps.
10. Report known limitations.

---

# Important Project Constraints

Do not:

* Replace Angular with React
* Add Tailwind CSS
* Add Angular Material
* Add another frontend framework
* Replace Bootstrap 5
* Convert standalone components to NgModules
* Change working API URLs without necessity
* Rename existing DTO fields unnecessarily
* Remove existing permission checks
* Weaken backend authorization
* Remove maker-checker workflow
* Replace Signals with another state-management library
* Add NgRx unless explicitly requested
* Add microservices
* Add event brokers such as Kafka or RabbitMQ unless explicitly requested
* Add multi-tenant SaaS complexity
* Add unnecessary design patterns
* Hardcode financial values
* Use mock financial data in completed features
* Commit generated build files
* Commit uploaded user files
* Expose passwords, tokens or secrets
* log sensitive authentication data

---

# Existing Business Modules

Before implementing a feature, inspect whether the following modules already exist and reuse them where appropriate:

* Authentication
* JWT and refresh token
* Users
* Roles
* Permissions
* Audit logs
* Chart of Accounts
* Journal Entries
* Parties
* Sales Invoices
* Vendor Bills
* Payments
* Payment Allocations
* Expenses
* Recurring Expenses
* Credit Notes
* Debit Notes
* Banking
* Bank Transactions
* Bank Reconciliation
* Fiscal Years
* Accounting Periods
* Budget Management
* Fixed Assets
* Depreciation
* Financial Reports
* Dashboard
* Notifications
* Settings

Do not create duplicate modules, services or APIs without checking the existing implementation.

---

# Backend Architecture Rules

Follow the existing package structure.

Typical package structure may include:

```text
controller
service
serviceimpl
repository
entity
dto
mapper
exception
enums
security
config
```

Do not introduce a different architectural style into one module unless the whole project already uses it.

Keep controllers thin.

Controllers should:

* Accept requests
* Validate authorization
* Delegate to services
* Return standardized responses

Controllers should not contain complex accounting or business logic.

Keep business logic inside service classes.

Repository classes should only handle persistence queries.

Do not call repositories directly from controllers unless the current project explicitly follows that pattern and changing it is outside the task scope.

---

# Spring Boot Coding Rules

Use constructor injection where possible.

Preferred:

```java
@RequiredArgsConstructor
@Service
public class ExampleServiceImpl implements ExampleService {

    private final ExampleRepository exampleRepository;
}
```

Avoid new field injection using `@Autowired`.

Use:

```java
org.springframework.transaction.annotation.Transactional
```

for service transaction management.

Use:

```java
@Transactional(readOnly = true)
```

for read-only service operations where appropriate.

Use `BigDecimal` for all financial values.

Never use:

```java
double
float
```

for money.

Use explicit scale and rounding when calculations require it.

Example:

```java
amount.setScale(2, RoundingMode.HALF_UP);
```

Do not silently round values without checking existing project rules.

Use `LocalDate` for accounting dates.

Use `LocalDateTime` for audit timestamps.

Use enums for controlled statuses and types.

Avoid storing enum-like values as arbitrary strings when an enum already exists.

Use meaningful business exceptions.

Examples:

```text
ResourceNotFoundException
BusinessRuleException
ValidationException
UnauthorizedOperationException
```

Reuse existing exception classes before creating new ones.

---

# Entity and Database Rules

Inspect existing entity conventions before adding fields.

Preserve:

* Existing table names
* Existing column names
* Existing relationships
* Existing audit fields
* Existing soft-delete behavior
* Existing company scoping

Do not drop tables or columns unless explicitly requested.

Do not delete existing user data.

Do not generate destructive SQL without clearly warning the user.

For database schema changes:

1. Explain why the change is required.
2. Show the exact entity changes.
3. Show the exact SQL migration if migrations are used.
4. Mention whether existing rows require backfilling.
5. Avoid making required columns non-null without a migration strategy.

Do not rely only on:

```properties
spring.jpa.hibernate.ddl-auto=update
```

for a production-style schema change without documenting the effect.

---

# Accounting Rules

Accounting correctness is more important than convenience.

Do not change accounting behavior unless the requested feature requires it.

Preserve double-entry accounting.

For posted journals:

```text
Total Debit = Total Credit
```

Reject unbalanced entries.

Do not allow financial balances to be updated through arbitrary frontend values.

Financial balances must come from verified backend business logic.

Posted transactions should not be directly edited unless the existing workflow explicitly allows it.

Use cancellation or reversal where the project already follows that approach.

Preserve journal source references.

Examples:

```text
INVOICE
VENDOR_BILL
PAYMENT
EXPENSE
MANUAL
CREDIT_NOTE
DEBIT_NOTE
FIXED_ASSET
```

Avoid creating duplicate journals for the same source.

Before posting, verify whether a journal already exists for the source.

Respect accounting period validation.

Do not post into a closed or locked accounting period if the existing system enforces period locking.

---

# Journal Rules

Journal entries must:

* Contain at least two valid lines
* Use active ledger accounts
* Avoid parent accounts if parent accounts are non-postable
* Have positive debit or credit values
* Not have both debit and credit on the same line unless existing rules permit it
* Balance before posting
* Preserve source type and source ID
* Prevent duplicate posting
* Record audit information

Reversal entries should:

* Swap debit and credit
* Reference the original journal
* Preserve transaction traceability
* Avoid deleting the original posted journal

---

# Invoice Rules

Preserve the current invoice workflow.

Typical statuses may include:

```text
DRAFT
APPROVED
POSTED
PARTIAL
PAID
CANCELLED
```

Confirm the actual enum before making changes.

Invoice posting generally creates:

```text
Debit: Accounts Receivable
Credit: Sales Revenue
Credit: Output VAT, when applicable
```

Do not invent account IDs.

Use configured default accounts or existing account mappings.

Payment allocations must correctly update:

* Paid amount
* Due amount
* Payment status

Do not allow allocations to exceed the invoice due amount.

---

# Vendor Bill Rules

Confirm the existing workflow before implementation.

Vendor bill posting generally creates:

```text
Debit: Expense or Asset
Debit: Input VAT, when applicable
Credit: Accounts Payable
Credit: TDS Payable, when applicable
```

Do not hardcode account IDs.

Use configured default accounts.

Do not allow payment allocation to exceed the remaining vendor bill due amount.

---

# Payment Rules

Preserve existing payment types:

```text
RECEIPT
PAYMENT
```

Typical accounting:

Customer receipt:

```text
Debit: Cash or Bank
Credit: Accounts Receivable
```

Vendor payment:

```text
Debit: Accounts Payable
Credit: Cash or Bank
```

Payment allocation must:

* Match the correct party
* Match the correct reference type
* Not exceed payment amount
* Not exceed document due amount
* Avoid cancelled documents
* Avoid duplicate allocation

Confirm whether payments can be cancelled after posting before changing behavior.

---

# Banking Rules

If a Chart of Account is linked to a bank account, inspect the existing bank mirror implementation.

Do not create duplicate bank transactions.

For journal-generated bank transactions:

* Preserve source reference
* Use correct debit or credit direction
* Update the bank balance consistently
* Reverse or cancel the mirrored bank transaction when the journal is reversed

Do not independently update the bank account balance from the frontend.

---

# Budget Rules

Preserve the existing budget design.

Current expected behavior:

* Annual and monthly budgeting
* Expense and revenue accounts
* Soft warning when budget is exceeded
* Budget warning should not automatically block a transaction unless the current implementation explicitly does so

Do not convert soft warnings into hard validation without explicit instruction.

Reuse:

* BudgetCheckService
* BudgetWarningDto
* Existing budget variance calculations
* Existing notification flow

---

# Maker-Checker Rules

The project already has a maker-checker workflow.

Keep the current maker-checker implementation unchanged unless the user explicitly asks for a fix.

Do not:

* Add multi-level approval
* Add complicated approval matrices
* Change status flow
* Add new approval tables
* Prevent current working flows

The project is a personal portfolio project, not a client production system.

Avoid unnecessary approval complexity.

---

# Permission Rules

Backend permission checks are mandatory.

Frontend permission visibility is not a replacement for backend authorization.

Every protected backend endpoint should use the existing authorization approach, such as:

```java
@PreAuthorize("hasAuthority('VIEW_INVOICE')")
```

Confirm actual permission names before using them.

Do not invent new permission names if a suitable permission already exists.

Frontend pages should use:

* Existing permission guard
* Existing permission directive
* Existing auth service
* Existing permission constants

Routes must remain protected.

Buttons and menu items must be hidden when permission is missing.

Direct API access must still be blocked by the backend.

Viewer-style roles should not see create, edit, post, delete or cancel actions unless they have those permissions.

---

# Angular Architecture Rules

Use Angular standalone components.

Do not introduce NgModules unless required by an existing library.

Use Signals where the project already uses them.

Example:

```typescript
loading = signal(false);
data = signal<ExampleDto[]>([]);
errorMessage = signal<string | null>(null);
```

Use Reactive Forms for forms.

Use typed interfaces for API data.

Avoid `any`.

Preferred:

```typescript
interface InvoiceResponse {
  id: number;
  invoiceNumber: string;
}
```

Avoid:

```typescript
response: any
```

Inspect whether the API response is wrapped before accessing:

```typescript
response.data
```

Do not assume all responses use a wrapper.

Match the actual backend response.

Keep components manageable.

If a component becomes too large, extract reusable UI sections only when it improves readability.

Do not over-componentize simple pages.

---

# Angular Service Rules

Keep API calls inside services.

Use the existing API base URL configuration.

Do not hardcode:

```text
http://localhost:8085
```

inside individual components.

Reuse environment configuration or the existing API configuration service.

Use strongly typed service methods.

Example:

```typescript
getById(id: number): Observable<InvoiceResponseDto>
```

Handle API errors consistently.

Do not subscribe inside services unless the existing design specifically requires it.

---

# Angular Template Rules

Use Angular 21 control flow where the project already uses it:

```html
@if
@else
@for
```

Avoid mixing old and new template syntax unnecessarily.

Use semantic HTML.

Maintain accessibility:

* Labels for inputs
* Button types
* Descriptive titles
* Keyboard support
* Appropriate ARIA attributes where useful
* Sufficient contrast

Do not call expensive functions repeatedly from templates.

Use computed signals or prepared values where appropriate.

---

# Styling Rules

Use Bootstrap 5 and project SCSS.

Do not add inline styling unless necessary.

Do not add random colors that conflict with the current design system.

Reuse existing:

* CSS variables
* Spacing
* Button classes
* Cards
* Status badges
* Form styles
* Table styles

New designs should be:

* Professional
* Responsive
* Consistent
* Finance-focused
* Visually clean
* Not overly animated

Use subtle animations only.

Respect:

```css
@media (prefers-reduced-motion: reduce)
```

Print styles must hide:

* Sidebar
* Header
* Navigation
* Interactive buttons

Printed reports should be A4-friendly.

---

# Dashboard Rules

Dashboard values must come from backend APIs.

Do not use hardcoded KPI values.

Before changing the dashboard:

1. Inspect DashboardSummaryDto.
2. Inspect dashboard service implementation.
3. Inspect frontend dashboard service.
4. Inspect dashboard model.
5. Inspect dashboard component.
6. Inspect dashboard template.
7. Inspect permission directives.

Use all meaningful existing DTO fields.

Do not display sensitive information to roles without permission.

Dashboard should include appropriate states:

* Loading
* Success
* Empty
* Error
* Retry

Charts must handle missing or zero data safely.

---

# Notification Rules

Reuse the existing notification system.

Do not create a second notification framework.

Notifications should contain enough information to navigate to the related record when supported.

Possible fields may include:

```text
type
title
message
priority
entityType
entityId
isRead
readAt
createdAt
```

Confirm actual entity and DTO fields before implementation.

Avoid generating duplicate notifications for the same event.

Notification failure should not roll back a successful core financial transaction unless the current architecture explicitly requires it.

---

# Audit Log Rules

Audit logs should record important actions without exposing sensitive data.

Useful audit actions include:

* Created
* Updated
* Posted
* Approved
* Cancelled
* Reversed
* Login
* Failed login
* Permission change

Do not log:

* Passwords
* Raw JWT tokens
* Refresh tokens
* Secret keys
* Complete authentication headers

Mask sensitive fields in before-and-after data.

Activity timeline features should reuse audit logs where possible.

Do not create duplicate activity records if audit data already supports the feature.

---

# Error Handling Rules

Reuse the existing global exception handler.

Use a consistent backend error response.

Example structure:

```json
{
  "success": false,
  "message": "Accounting period is closed",
  "errorCode": "ACCOUNTING_PERIOD_CLOSED",
  "timestamp": "2026-07-25T15:30:00"
}
```

Confirm the project’s existing response format before changing it.

Frontend should handle:

```text
400 — Validation error
401 — Session expired
403 — Permission denied
404 — Resource not found
409 — Business rule conflict
500 — Unexpected server error
```

Show user-friendly messages.

Do not expose stack traces in the UI.

---

# File Upload Rules

Before adding file uploads, inspect whether upload handling already exists.

Reuse existing storage conventions where safe.

Validate:

* Maximum file size
* File extension
* MIME type
* Entity ownership
* User permission

Do not store uploaded user files inside source-controlled directories.

Do not commit uploaded files.

Use safe generated storage names.

Preserve original filenames only as metadata.

Block executable files.

---

# Reporting Rules

Reports must be calculated from verified accounting data.

Do not calculate official report balances only from frontend data.

Use backend calculations.

Use `BigDecimal`.

Date filters must be respected.

Reports should handle:

* Empty results
* Invalid date ranges
* Closed periods
* Zero balances
* Negative balances
* Debit and credit natural balances

Historical reports should use transaction dates and posted journal data where appropriate.

Do not use only the current stored account balance for an historical `asOfDate` report unless the existing report explicitly documents that limitation.

---

# Export Rules

PDF, Excel and CSV exports should reuse existing report data.

Avoid recalculating the same report differently in export code.

Export results should match the on-screen report.

Include:

* Company name when available
* Report title
* Date range
* Generated date
* Proper numeric formatting
* Total rows

Do not include sidebar, buttons or unrelated page elements in print output.

---

# Testing Rules

Do not only verify that code compiles.

Test business behavior.

Priority backend tests:

* Balanced journal posts
* Unbalanced journal is rejected
* Duplicate posting is prevented
* Invoice posting creates the correct journal
* Vendor bill posting creates the correct journal
* Customer receipt reduces invoice due
* Vendor payment reduces bill due
* Reversal restores accounting effect
* Closed period prevents posting
* Permission checks block unauthorized access
* Budget warning is returned without blocking
* Linked bank transactions remain consistent

Use unit tests for isolated logic.

Use integration tests for cross-module workflows.

Frontend tests should focus on:

* Guards
* Permission visibility
* Form validation
* Service response mapping
* Error states
* Status-based actions

Do not create meaningless tests only for coverage numbers.

---

# Build and Verification Commands

Inspect project files before choosing commands.

Typical backend commands:

```bash
cd backend
mvn clean compile
mvn test
```

On Windows, Maven wrapper may be used:

```bash
mvnw.cmd clean compile
mvnw.cmd test
```

Typical frontend commands:

```bash
cd nexa-erp-frontend
npm install
npm run build
npm test
```

Do not repeatedly run `npm install` unless dependencies changed or `node_modules` is missing.

Use the scripts defined in `package.json`.

---

# Git Rules

Before making changes, check:

```bash
git status
```

Do not overwrite unrelated uncommitted user changes.

Do not reset, clean or discard files without explicit permission.

Do not run destructive Git commands such as:

```bash
git reset --hard
git clean -fd
```

Do not automatically commit unless explicitly requested.

At completion, provide:

```text
Files created
Files modified
Files deleted
Build result
Test result
Suggested commit message
```

Suggested commit format:

```text
feat: add notification center
fix: correct invoice allocation logic
refactor: simplify dashboard data mapping
test: add journal posting integration tests
docs: improve project documentation
chore: update project configuration
```

---

# Feature Implementation Workflow

For every feature, follow this process.

## Phase 1 — Inspect

* Search relevant entities
* Search DTOs
* Search repositories
* Search services
* Search controllers
* Search permissions
* Search frontend routes
* Search frontend services
* Search frontend models
* Search frontend pages
* Search existing tests

## Phase 2 — Plan

Provide a concise plan containing:

* Existing code to reuse
* Files to modify
* Files to create
* API changes
* Database changes
* Permission changes
* Main risks
* Test plan

## Phase 3 — Implement Backend

* Make minimal changes
* Preserve existing business rules
* Add validation
* Add authorization
* Add tests where practical
* Avoid unrelated refactoring

## Phase 4 — Verify Backend

* Compile
* Run tests
* Report failures
* Fix only relevant issues

## Phase 5 — Implement Frontend

* Update typed models
* Update service methods
* Add route permissions
* Build UI
* Add loading state
* Add empty state
* Add error state
* Add responsive behavior

## Phase 6 — Verify Frontend

* Run production build
* Run tests where available
* Check for template errors
* Check for TypeScript errors
* Check console errors
* Check responsive layout
* Check permission visibility

## Phase 7 — Final Report

Provide:

1. Summary of completed work
2. Exact files created
3. Exact files modified
4. Exact files deleted
5. API changes
6. Database changes
7. Permission changes
8. Build results
9. Test results
10. Manual test steps
11. Known limitations
12. Suggested commit message

Then stop.

---

# Current Development Priority

Implement features one at a time in this order unless the user requests otherwise:

1. Clean project baseline
2. Dashboard polish
3. Notification Center
4. Activity Timeline
5. Global Search
6. PDF and Excel export
7. Cash Flow Statement
8. Comparative Reports
9. Expense and Vendor Bill attachments
10. User Profile
11. Company Settings
12. Swagger/OpenAPI
13. Automated Tests
14. Root README and GitHub documentation

Do not automatically continue to the next feature.

---

# Portfolio Project Priorities

Prefer improvements that demonstrate:

* Java and Spring Boot skills
* Angular skills
* REST API design
* Authentication
* Authorization
* Accounting business logic
* Database design
* Error handling
* Testing
* Responsive UI
* Documentation

Avoid increasing scope only to make the project appear larger.

A smaller complete and reliable system is better than many incomplete modules.

When choosing between adding a new module and polishing a working module, prefer completing and polishing the working module.

---

# Final Safety Rule

If a requested change may break accounting correctness, permissions, authentication, data integrity or existing workflows:

1. Stop before applying the risky part.
2. Explain the risk clearly.
3. Suggest the smallest safe alternative.
4. Do not silently change the behavior.

Never claim a build, test or feature is successful unless it was actually verified.
