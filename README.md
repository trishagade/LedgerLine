# LedgerLine — Double-Entry Banking System

LedgerLine is a full-stack banking application built to demonstrate secure account management, money movement, double-entry bookkeeping, idempotent transactions, and audit logging.

## Live Demo

- **Frontend:** https://ledgerline-frontend-ax4v.onrender.com
- **Backend:** https://ledgerline-backend-863k.onrender.com
- **GitHub Repository:** https://github.com/trishagade/LedgerLine

## Features

- User registration and login
- Role-based access control for Customers and Admins
- Create and manage customer accounts
- Deposit and withdrawal operations
- Transfer money between accounts
- Double-entry ledger for recording transactions
- Idempotency protection against duplicate transaction requests
- Insufficient funds validation
- Admin dashboard to view all accounts
- Append-only audit log for tracking important actions
- Password hashing and token-based authentication

## Technology Stack

**Frontend**
- React
- Vite
- JavaScript
- CSS

**Backend**
- Java 21
- Spring Boot
- Spring Security
- Maven

**Database**
- PostgreSQL

**Deployment**
- Render
- Docker

## Application Roles

### Customer
- Register and log in
- Create accounts
- Deposit and withdraw money
- Transfer funds
- View account balances and transaction history

### Admin
- View all accounts
- Access the administrative dashboard
- Review the audit log

## Running Locally

### Prerequisites
- Java 21
- Maven
- Node.js and npm
- PostgreSQL

### Backend Setup

1. Create a PostgreSQL database named `ledgerline_db`.
2. Configure the required environment variables:

```text
DB_PASSWORD=your_database_password
LEDGERLINE_ADMIN_USERNAME=your_admin_username
LEDGERLINE_ADMIN_PASSWORD=your_admin_password
```

3. Open the backend directory and run:

```bash
cd backend
mvn spring-boot:run
```

### Frontend Setup

Open another terminal:

```bash
cd frontend
npm install
npm run dev
```

Open the local frontend at `http://localhost:5173`.

## Security and Reliability

- Passwords are stored using BCrypt hashing.
- Protected API endpoints require authentication.
- Administrative endpoints enforce server-side role checks.
- Idempotency keys help prevent duplicate processing of the same transaction request.
- Audit records capture successful and failed activities.
- Double-entry accounting maintains balanced transaction records.

## Project Purpose

This project was developed as part of the Snovasys Fresher Hiring Challenge to demonstrate full-stack development, secure money movement, transaction integrity, and deployment skills.

---

**Note:** LedgerLine is an educational project and is not intended for handling real-world banking transactions.
