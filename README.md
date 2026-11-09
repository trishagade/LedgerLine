# LedgerLine

**A secure banking application with account management, financial transactions, and a double-entry ledger.**

LedgerLine is a full-stack banking-style application built using Java Spring Boot and React. It supports customer accounts, deposits, withdrawals, transfers, transaction history, and audit logging.

## Live Application

- **Frontend:** https://ledgerline-frontend-ax4v.onrender.com
- **Backend:** https://ledgerline-backend-863k.onrender.com

The backend may take some time to wake up when using a free hosting instance.

## Features

### Customer

- Register with full name, email, password, and optional mobile number.
- Log in using email or username.
- Create accounts with automatically generated account numbers.
- View personal accounts and balances.
- Deposit and withdraw funds.
- Transfer funds between accounts.
- View account transaction history and ledger entries.

### Administrator

- Log in using an ADMIN account.
- View all customer accounts.
- View the audit log.

### Security and Integrity

- Spring Security authentication.
- Role-based authorization for administrative endpoints.
- Customer account ownership checks.
- BCrypt password hashing.
- Input validation for registration.
- Insufficient-funds validation.
- Idempotency handling for duplicate transaction requests.
- Append-only audit logging for important events.
- Double-entry ledger records for financial transactions.

## Technology Stack

| Layer | Technology |
|---|---|
| Frontend | React, JavaScript, Vite |
| Backend | Java 21, Spring Boot |
| Security | Spring Security, BCrypt |
| Database | PostgreSQL |
| Build | Maven |
| Hosting | Render |
| Version Control | Git and GitHub |

## Application Architecture

```text
React Frontend
      |
      | REST API / Bearer Authentication
      |
Spring Boot Backend
      |
      | JPA / Hibernate
      |
PostgreSQL Database
```

## Local Setup

### Prerequisites

- Java 21
- Node.js and npm
- PostgreSQL
- Git

### 1. Clone the repository

```bash
git clone https://github.com/trishagade/LedgerLine.git
cd LedgerLine
```

### 2. Configure the backend

Configure the required PostgreSQL connection settings using environment variables:

- `SPRING_DATASOURCE_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Navigate to the backend folder:

```bash
cd backend
```

Compile the application:

```bash
.\mvnw.cmd compile
```

Run the application:

```bash
.\mvnw.cmd spring-boot:run
```

### 3. Configure and run the frontend

Open another terminal:

```bash
cd frontend
npm install
```

Create a `.env` file with the backend URL:

```env
VITE_API_BASE=http://localhost:8080
```

Start the frontend:

```bash
npm run dev
```

Open the local URL displayed by Vite in your browser.

## Testing

The following application workflows have been exercised:

- Customer registration and login.
- Administrator login.
- Account creation and automatic account numbering.
- Deposits, withdrawals, and transfers.
- Insufficient-funds rejection.
- Ledger balance updates.
- Audit event recording.
- Duplicate idempotency request handling locally.
- Customer account retrieval.

Some additional security tests, including direct customer-to-admin API denial, remain pending.

## Project Documentation

- `PROMPTS.md` — AI-assisted development and verification log.
- `THREATS.md` — Threat model, mitigations, and security testing status.

## Important

Never commit database passwords, authentication secrets, API tokens, or other private environment values to GitHub. Configure secrets through environment variables.

## Author

**Trisha Gade**

GitHub: https://github.com/trishagade