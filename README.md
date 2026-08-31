# NexaERP — Accounting & Finance Management System

A modular, full-stack **Accounting & Finance ERP** built for small and medium-sized businesses in Bangladesh.

NexaERP is designed and developed end-to-end across the **backend, web frontend, and companion mobile applications**, with a strong focus on accounting correctness, security, maintainable architecture, automated testing, reporting, and real-world business workflows.

> ⚠️ **Portfolio Project**
>
> NexaERP is a personal portfolio project, not a live production system for a real client. It is intentionally engineered toward production-quality standards — including double-entry accounting, role-based security, approval workflows, audit trails, automated tests, and financial controls — to demonstrate real-world full-stack engineering capabilities.

Project Video Link- https://youtu.be/XazqMrvjvug?si=9zTRrqX9aV44zvBm

---

## 📌 Project Overview

NexaERP brings core accounting and business operations into a centralized ERP platform.

The system covers:

* Accounting & bookkeeping
* Sales & purchasing
* Customer & vendor management
* Invoicing & payments
* Banking & reconciliation
* Budgeting
* Financial reporting
* Approval workflows
* User, role & permission management
* Audit logging
* Automated business processes
* PDF & Excel reporting
* System configuration

The application is built around **real accounting and financial workflows rather than simple CRUD operations**.

---

## 💡 Why This Project?

Many portfolio ERP applications focus primarily on CRUD screens.

NexaERP focuses on the **business rules and correctness behind those screens**.

The system includes concepts such as:

* Double-entry journal posting
* FIFO payment allocation
* Manual payment allocation
* Maker–Checker approval workflows
* Accounting period locking
* Bank reconciliation
* Transaction reversal and voiding
* Audit logging
* Multi-currency handling
* Budget monitoring
* Automated recurring transactions
* Financial statement generation

These workflows are designed to demonstrate how an enterprise-style business application can be structured around **financial correctness, security, traceability, and maintainability**.

---

# 🛠️ Tech Stack

## Backend

* **Java 21**
* **Spring Boot 4**
* Spring Security
* JWT Authentication
* Access & Refresh Tokens
* HttpOnly Cookie-based authentication flow
* Login lockout after repeated failures
* Spring Data JPA
* Hibernate
* MySQL
* Maven
* Lombok
* Bean Validation
* Caching
* Scheduled Tasks
* RESTful APIs
* **iText 7** — PDF generation
* **Apache POI** — Excel export
* **JUnit** — automated backend testing

### Backend Testing

The project currently contains **35 backend test classes** covering critical areas including:

* Services
* Controllers
* Business logic
* Approval workflows
* Validation
* Accounting-related operations

---

## Frontend

* **Angular 21**
* TypeScript
* Standalone Components
* Angular Signals
* Reactive Forms
* Angular Router
* Lazy-loaded feature routes
* Route Guards
* JWT Authentication Interceptor
* Bootstrap 5
* Bootstrap Icons
* SCSS
* RxJS
* ApexCharts

The frontend follows a modular feature-based structure with reusable services, guards, interceptors, forms, and UI components.

---

## Mobile

NexaERP is also being extended with companion mobile applications that consume the same backend API.

### Android

* Java
* MVVM architecture
* Repository Pattern
* Retrofit
* OkHttp
* LiveData
* RecyclerView

### Flutter

* Flutter / Dart
* REST API integration
* Companion mobile application
* **Currently in active development**

---

# 🧩 Core Modules

## 📚 Accounting Core

The accounting module forms the foundation of NexaERP.

### Chart of Accounts

* Hierarchical Chart of Accounts
* Account types
* Parent/child account relationships
* Account status management
* Configurable default accounts

### Double-Entry Accounting

* Manual Journal Entries
* Debit/Credit validation
* Automatic posting
* Journal status management
* Accounting transaction tracking
* Opening balances
* Ledger generation

All monetary calculations use `BigDecimal` with explicit rounding rules to avoid floating-point precision issues.

---

## 📅 Fiscal Years & Accounting Periods

NexaERP supports controlled accounting periods with period-closing checks.

The closing process can validate conditions such as:

* Unposted journal entries
* Draft invoices
* Draft vendor bills
* Draft payments
* Unreconciled bank items
* Negative cash conditions
* Other pending accounting activities

This helps prevent accidental modifications to completed accounting periods.

---

# 🧾 Sales & Purchasing

## Sales Invoices

* Customer invoices
* Invoice line items
* Tax calculation
* Invoice posting
* Invoice approval
* Invoice status tracking
* Payment tracking
* PDF generation

## Vendor Bills

* Vendor bills
* Bill line items
* Tax handling
* Bill approval
* Bill posting
* Payment tracking
* Vendor statement

## Credit & Debit Notes

* Credit Notes
* Debit Notes
* Transaction adjustments
* Accounting integration

---

# 👥 Customers & Vendors

The system provides centralized party management for customers and vendors.

Features include:

* Customer management
* Vendor management
* Contact information
* Opening balances
* Transaction history
* Party statements
* Outstanding balance tracking
* Aging analysis

---

# 💳 Payments

NexaERP supports both automated and manual payment allocation.

### Payment Allocation

* FIFO-based automatic allocation
* Manual allocation
* Partial payment handling
* Outstanding balance calculation
* Payment-to-invoice mapping

### Payment Workflow

```text
Payment Created
      │
      ▼
Validation
      │
      ▼
Approval (if required)
      │
      ▼
Posting
      │
      ▼
Allocation
      │
      ▼
Account Balance Updated
```

---

# 🏦 Banking & Reconciliation

The banking module supports common business banking workflows.

### Bank Accounts

* Bank account management
* Account balances
* Bank transactions

### Reconciliation

* Manual transaction matching
* CSV bank statement upload
* Reconciliation tracking
* Unreconciled transaction detection

### Bank Operations

* Bank-to-bank transfer
* Transaction void
* Transaction reversal
* Reconciliation status management

---

# 💱 Multi-Currency

NexaERP supports multi-currency financial transactions.

Features include:

* Currency management
* Exchange rate management
* Foreign currency transactions
* Currency-aware financial calculations

---

# 📊 Financial Reporting

NexaERP provides financial and operational reporting across multiple modules.

## Core Financial Reports

* General Ledger
* Trial Balance
* Profit & Loss Statement
* Balance Sheet
* Cash Flow Statement

## Party Reports

* Party Statement
* Aging Report

## Management Reports

* Budget vs Actual
* Cost Center Report
* Account reports
* Outstanding receivables
* Outstanding payables

---

## 📋 Party Statement

The Party Statement provides transaction-level customer/vendor history.

| Date             | Type             | Reference |        Debit |        Credit |         Balance |
| ---------------- | ---------------- | --------- | -----------: | ------------: | --------------: |
| Transaction Date | Transaction Type | Reference | Debit Amount | Credit Amount | Running Balance |

---

## 📅 Aging Report

The Aging Report provides due-date based outstanding analysis.

| Customer   | Current | 1–30 Days | 31–60 Days | 60+ Days | Total Due |
| ---------- | ------: | --------: | ---------: | -------: | --------: |
| Customer A |       — |         — |          — |        — |         — |

---

## 📤 Export & Documents

Financial reports can be exported into common business formats.

### Excel

**Apache POI** is used for Excel report generation.

Supported reporting workflows include Excel exports for financial and operational reports.

### PDF

**iText 7** is used for PDF document generation.

PDF generation is available for applicable business documents and reports.

---

# 💰 Budget Management

NexaERP includes budgeting and monitoring capabilities.

Features include:

* Annual budgets
* Budget allocation
* Period-based budget amounts
* Budget vs Actual reporting
* Budget monitoring
* Budget validation
* Budget alerts

The system can also automate notifications for relevant budget-related conditions.

---

# 🎯 Cost Centers

Cost centers allow financial transactions to be analyzed by business unit or operational area.

The system supports:

* Cost center management
* Transaction-level cost center assignment
* Cost center propagation across financial documents
* Cost center reporting

Cost center information can flow through invoices, expenses, and journal transactions.

---

# ✅ Maker–Checker Approval Workflow

NexaERP implements a reusable **Maker–Checker approval workflow**.

The approval engine is designed to work across multiple document types.

Currently supported workflows include:

* Invoices
* Vendor Bills
* Payments
* Manual Journal Entries

### Approval Flow

```text
Draft
  │
  ▼
Submitted
  │
  ▼
Pending Approval
  │
  ├──────────────► Rejected
  │
  ▼
Approved
  │
  ▼
Posted
```

The workflow is designed to separate transaction creation from transaction approval, providing stronger internal controls.

---

# 🔐 Security & Authorization

Security is implemented using **Spring Security + JWT**.

### Authentication

* JWT access tokens
* Refresh tokens
* HttpOnly cookie authentication flow
* Login protection
* Failed login attempt tracking
* Account lockout handling

### Authorization

NexaERP uses database-driven **Role-Based Access Control (RBAC)**.

Permissions are enforced at the API level using Spring Security authorization mechanisms such as:

```java
@PreAuthorize(...)
```

This ensures that authorization is enforced by the backend rather than relying only on frontend visibility.

---

# 👤 User & Role Management

The system includes centralized user and access management.

Features:

* User creation
* User invitation
* Email verification
* Password reset
* Role assignment
* Permission management
* User status management
* Permission-based feature access

---

# 📝 Audit Logging

Important business operations are tracked through audit logs.

Audit capabilities include:

* User/action tracking
* Per-action IP tracking
* Sensitive-field masking
* Operation history
* Business activity traceability

This provides additional visibility into who performed sensitive operations and when.

---

# ⏰ Automation & Scheduled Processes

NexaERP includes scheduled background processes for repetitive business operations.

Examples include:

* Recurring journal entries
* Budget alerts
* Overdue payment detection
* Email notifications
* Automated financial reminders

The goal is to reduce repetitive manual work and improve consistency.

---

# 🏢 Fixed Assets

The system includes fixed asset management capabilities.

Features include:

* Asset records
* Asset categorization
* Depreciation handling
* Asset-related accounting

---

# 🔎 Global Search

NexaERP provides global search capabilities across supported modules.

The goal is to allow users to quickly locate relevant:

* Customers
* Vendors
* Invoices
* Bills
* Payments
* Accounts
* Other business records

---

# 📈 Dashboard

The dashboard provides a centralized overview of business and financial activity.

Key elements include:

* KPI cards
* Revenue overview
* Expense overview
* Financial trends
* 6-month revenue vs expense chart
* Attention Center

### Attention Center

The dashboard highlights items that may require user action or review, helping users identify pending operational tasks.

---

# 🇧🇩 Built with Bangladesh SMEs in Mind

NexaERP is designed with the workflow and localization requirements of Bangladeshi SMEs in mind.

The system considers concepts such as:

* BDT currency formatting
* NBR VAT/Mushak reporting concepts
* Local business workflows
* bKash payment methods
* Nagad payment methods

The goal is to explore how an ERP can be adapted to the practical requirements of businesses operating in Bangladesh.

> Note: Local tax and compliance features are implemented as application concepts for this portfolio project and should not be treated as official tax/compliance advice.

---

# 🏗️ Architecture

NexaERP follows a layered architecture designed to keep controllers, business logic, persistence, and domain models separated.

```text
┌─────────────────────────────────────────────┐
│              Angular Frontend               │
│                                             │
│ Components • Services • Guards • Interceptor│
└──────────────────────┬──────────────────────┘
                       │
                       │ REST API
                       ▼
┌─────────────────────────────────────────────┐
│              Spring Boot Backend            │
│                                             │
│ Controllers                                 │
│      ↓                                      │
│ Services / ServiceImpl                      │
│      ↓                                      │
│ Repositories                                │
│      ↓                                      │
│ JPA Entities / Database                     │
└──────────────────────┬──────────────────────┘
                       │
                       │ Hibernate / JPA
                       ▼
┌─────────────────────────────────────────────┐
│                    MySQL                    │
└─────────────────────────────────────────────┘
```

---

# 🧱 Backend Architecture Principles

### Layered Design

The backend follows a consistent structure:

```text
Controller
    ↓
Service / ServiceImpl
    ↓
Repository
    ↓
Entity
```

DTOs are used to separate API contracts from persistence models where appropriate.

### Constructor Injection

Dependencies are injected through constructors rather than field-level `@Autowired`.

### Thin Controllers

Controllers are intentionally kept thin.

Business rules and accounting logic are handled inside service layers.

### Monetary Precision

All financial amounts use:

```java
BigDecimal
```

rather than:

```java
float
double
```

This avoids floating-point precision problems in financial calculations.

### Configuration

Important system defaults are configurable through dynamic system settings rather than being unnecessarily hardcoded into business logic.

---

# 📐 Backend Scale

Current backend structure includes approximately:

* **36 REST controllers**
* **~40 backend packages**
* Core shared domain entities
* Module-specific entities
* Service and repository layers
* Automated test coverage for critical workflows

The architecture continues to evolve as new modules and business rules are added.

---

# 📱 Multi-Client Architecture

NexaERP is designed around a shared REST API so multiple clients can consume the same backend.

```text
                  ┌──────────────────┐
                  │   Angular Web    │
                  │     Client       │
                  └────────┬─────────┘
                           │
                           │
┌──────────────────┐       │       ┌──────────────────┐
│ Android Client   │───────┼───────│ Flutter Client   │
└──────────────────┘       │       └──────────────────┘
                           │
                           ▼
                  ┌──────────────────┐
                  │   Spring Boot    │
                  │    REST API      │
                  └────────┬─────────┘
                           │
                           ▼
                  ┌──────────────────┐
                  │      MySQL       │
                  └──────────────────┘
```

This allows the web and mobile clients to share the same core business logic and backend services.

---

# 📸 Screenshots

> Screenshots will be added as the UI continues to be refined.

## Dashboard

![NexaERP Dashboard](docs/screenshots/dashboard.png)

## Chart of Accounts

![NexaERP Chart of Accounts](docs/screenshots/chart-of-accounts.png)

## Journal Entry

![NexaERP Journal Entry](docs/screenshots/journal-entry.png)

## Invoice

![NexaERP Invoice](docs/screenshots/invoice.png)

## Financial Reports

![NexaERP Financial Reports](docs/screenshots/financial-reports.png)

> Replace the image paths above with the actual screenshot files committed to the repository.

---

# 📂 Project Structure

```text
NexaERP/
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   ├── pom.xml
│   └── ...
│
├── nexa-erp-frontend/
│   ├── src/
│   ├── angular.json
│   ├── package.json
│   └── ...
│
├── docs/
│   └── screenshots/
│
├── README.md
└── ...
```

The exact project structure may evolve as development continues.

---

# ⚙️ Getting Started

## Requirements

Install the following before running the project:

* Java 21
* Maven
* Node.js
* Angular CLI 21
* MySQL
* Git

---

## 1. Clone the Repository

```bash
git clone https://github.com/md-sabbir-hasan/NexaERP.git
cd NexaERP
```

---

# 2. Backend Setup

Navigate to the backend:

```bash
cd backend
```

Create your local application configuration from the provided example configuration:

```text
application-example.properties
        ↓
application.properties
```

Configure your:

* MySQL database
* Database username
* Database password
* JWT configuration
* Mail configuration
* Other environment-specific settings

### Example

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/nexa_erp
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

> Never commit real passwords, JWT secrets, API keys, or other sensitive credentials to GitHub.

Run the backend:

```bash
mvn spring-boot:run
```

Backend:

```text
http://localhost:8085
```

---

# 3. Frontend Setup

Open a new terminal and navigate to:

```bash
cd nexa-erp-frontend
```

Install dependencies:

```bash
npm install
```

Start Angular development server:

```bash
ng serve
```

Frontend:

```text
http://localhost:4200
```

---

# 🧪 Testing

Backend tests can be executed using Maven.

```bash
mvn test
```

The project includes automated tests covering critical application areas such as:

* Services
* Controllers
* Business logic
* Approval workflows
* Validation
* Accounting-related operations

Testing is continuously expanded alongside feature development.

---

# 🔄 Development History

NexaERP has gone through multiple development, QA, debugging, and refinement phases.

The project has been developed iteratively with continuous source-control history covering:

```text
Initial Development
       ↓
Feature Development
       ↓
QA / Integration
       ↓
Debugging
       ↓
Refactoring
       ↓
Feature Expansion
       ↓
Current Development
```

The current repository contains the active development history of the project.

---

# 🗺️ Roadmap

The following features are planned or deferred for future development:

### Inventory

* Product/item management
* Stock management
* Warehouse management
* Stock movement
* Inventory valuation

### Sales

* Sales Orders
* Quotations
* Order-to-Invoice workflow

### Purchasing

* Purchase Orders
* Goods Receipt
* Purchase-to-Bill workflow

### HRM

An Angular HRM prototype with Bangladesh-oriented payroll concepts already exists separately.

Planned areas include:

* Employee management
* Payroll
* NBR tax concepts
* Provident fund
* Festival bonus
* Attendance-related workflows

### Enterprise Features

* Multi-company support
* Multi-branch support
* Advanced inventory workflows
* Additional integrations

---

# 🎯 Project Goals

NexaERP is being developed to explore and demonstrate how a business application can combine:

```text
Accounting
    +
Business Logic
    +
Security
    +
Approval Workflows
    +
Reporting
    +
Automation
    +
Web Application
    +
Mobile Application
```

into a single modular ERP platform.

The long-term goal is to continue improving the system's:

* Reliability
* Maintainability
* Security
* Accounting correctness
* Test coverage
* User experience
* Localization
* Scalability

---

# 📊 What This Project Demonstrates

NexaERP demonstrates practical experience with:

* Enterprise-style backend development
* Spring Boot REST API design
* Spring Security & JWT
* Database-driven RBAC
* Angular application architecture
* MySQL relational data modeling
* Accounting business logic
* Double-entry bookkeeping
* Financial reporting
* Approval workflows
* Audit logging
* Scheduled background jobs
* PDF generation
* Excel generation
* Automated testing
* API-driven mobile applications
* Modular software architecture
* Bangladesh-focused business localization

---

# 👨‍💻 About the Developer

## Sabbir Hasan

Full-Stack / Backend Developer focused on building production-oriented business applications.

### Primary Technologies

```text
Java
Spring Boot
Angular
MySQL
Spring Security
REST APIs
JWT
Android
Flutter
```

### GitHub

https://github.com/md-sabbir-hasan

### LinkedIn

https://linkedin.com/in/md-sabbir-hasan-041499388

---

# ⭐ Why NexaERP?

NexaERP is intentionally more than a collection of CRUD screens.

It combines:

**Accounting + Security + Business Rules + Workflow + Reporting + Automation + Web + Mobile**

with a focus on correctness and maintainability.

The project is continuously evolving as new accounting workflows, reports, security controls, tests, and business features are implemented.

---

## 📄 License

This project is currently maintained as a personal portfolio and development project.

Unless otherwise stated, the source code is not intended for commercial redistribution or production deployment without permission from the author.
