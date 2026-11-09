# LedgerLine — AI Prompt Log

This document records AI-assisted development, debugging, and verification performed for LedgerLine.

## Entry 1 — Double-entry ledger implementation

- **Date:** 2026-11-09
- **Request:** Help design and implement accounts, deposits, withdrawals, transfers, and a double-entry ledger.
- **AI output summary:** Guidance for transaction processing, debit/credit ledger entries, balance updates, and transaction consistency.
- **What needed attention:** Transaction correctness had to be verified using actual account balances and ledger entries.
- **Correction / verification:** Tested deposit, withdrawal, and transfer operations. Verified that ledger debit and credit entries balanced.
- **Related commit:** [Build LedgerLine double-entry banking application](https://github.com/trishagade/LedgerLine/commit/43bb38b)

## Entry 2 — Authentication and role-based access control

- **Date:** 2026-11-09
- **Request:** Review authentication and restrict administrative endpoints to ADMIN users.
- **AI output summary:** Guidance for Spring Security rules, authenticated API access, ADMIN-only routes, and denying unauthorized requests.
- **What needed attention:** UI visibility alone is not sufficient to protect administrative operations; backend authorization is necessary.
- **Correction / verification:** Reviewed SecurityConfig rules: `/api/admin/**` requires ADMIN, `/api/**` requires authentication, and unmatched routes are denied.
- **Related commit:** [Build LedgerLine double-entry banking application](https://github.com/trishagade/LedgerLine/commit/43bb38b)

## Entry 3 — Audit logging and security events

- **Date:** 2026-11-09
- **Request:** Add audit records for important authentication and financial operations.
- **AI output summary:** Guidance for recording actor, action, outcome, target, details, and request IP.
- **What needed attention:** Failed payment attempts and denied access must not be treated as successful operations.
- **Correction / verification:** Tested successful deposits and transfers, an insufficient-funds withdrawal, login/logout events, and admin audit-log viewing. Confirmed corresponding records appeared in the audit log.
- **Related commit:** [Build LedgerLine double-entry banking application](https://github.com/trishagade/LedgerLine/commit/43bb38b)

## Entry 4 — Duplicate payment protection

- **Date:** 2026-11-09
- **Request:** Implement and verify idempotency so repeated payment requests do not execute twice.
- **AI output summary:** Guidance for using an idempotency key to identify duplicate requests.
- **What needed attention:** A repeated request must not create a second financial effect.
- **Correction / verification:** Tested duplicate request handling locally and checked that the balance was incremented only once.
- **Related commit:** [Build LedgerLine double-entry banking application](https://github.com/trishagade/LedgerLine/commit/43bb38b)

## Entry 5 — Customer registration with email

- **Date:** 2026-11-09
- **Request:** Improve registration with full name, email, optional mobile number, password, and password confirmation.
- **AI output summary:** Guidance for backend validation, email-based customer login, and frontend password confirmation.
- **What needed attention:** The first live registration attempt returned a 502 while the backend was still running the older deployment.
- **Correction / verification:** Updated the backend and frontend deployments. Successfully registered and logged in with a new customer email. Verified account creation afterward.
- **Related commit:** [Improve customer registration with email and profile details](https://github.com/trishagade/LedgerLine/commit/241368e)

## Entry 6 — Render deployment configuration

- **Date:** 2026-11-09
- **Request:** Diagnose deployment startup and port configuration.
- **AI output summary:** Guidance to align the Spring Boot default port and Docker exposed port with the Render service configuration.
- **What needed attention:** An earlier deployment encountered a port scan timeout. The exact original cause was not conclusively established.
- **Correction / verification:** Updated the port configuration, compiled the backend successfully, pushed the changes, and verified that the subsequent Render deployment succeeded.
- **Related commit:** [Align backend port configuration for Render](https://github.com/trishagade/LedgerLine/commit/c3a5d74)

---

**Note:** This log summarizes actual AI-assisted development and observed tests. It does not claim that every security test was completed. Direct customer-to-admin API denial verification remains pending.