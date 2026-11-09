# LedgerLine — Threat Model and Security Tests

## 1. Overview

LedgerLine is a banking-style application that supports customer accounts, deposits, withdrawals, transfers, and a double-entry ledger.

This document identifies important security threats, the protections implemented, and the tests performed or still pending.

## 2. Threats and Mitigations

### Threat 1: Unauthorized access to customer accounts

- **Risk:** An unauthenticated user may attempt to access protected account information.
- **Mitigation:** Spring Security requires authentication for `/api/**` endpoints, except explicitly permitted public endpoints.
- **Verification:** Customer login and authenticated account retrieval were tested successfully.
- **Status:** Basic authentication tested.

### Threat 2: Unauthorized administrative access

- **Risk:** A CUSTOMER may attempt to access administrative endpoints.
- **Mitigation:** `/api/admin/**` requires the ADMIN role. Other unmatched routes are denied.
- **Verification:** Admin login and audit-log access were tested. Direct CUSTOMER-to-admin API denial still needs a dedicated 403 test.
- **Status:** Backend rule reviewed; direct denial test pending.

### Threat 3: Access to another customer's accounts

- **Risk:** A customer may try to view or modify accounts belonging to another customer.
- **Mitigation:** Account ownership is enforced in the backend service.
- **Verification:** The customer account API returned only accounts belonging to the logged-in customer (user ID 3).
- **Status:** Customer account retrieval tested; cross-customer manipulation test pending.

### Threat 4: Duplicate financial transactions

- **Risk:** Network retries or repeated requests may execute the same payment more than once.
- **Mitigation:** Idempotency handling prevents a repeated request with the same idempotency key from applying the financial operation twice.
- **Verification:** Duplicate request handling was tested locally; the balance changed only once.
- **Status:** Local test passed; live verification pending.

### Threat 5: Insufficient funds

- **Risk:** A withdrawal or transfer may reduce an account balance below zero.
- **Mitigation:** The backend validates available funds before completing the operation.
- **Verification:** A withdrawal exceeding the available balance was rejected. The balance remained unchanged and a failed payment audit event was recorded.
- **Status:** Tested.

### Threat 6: Password exposure

- **Risk:** Storing passwords in plain text could expose customer credentials.
- **Mitigation:** BCrypt password hashing is used for password storage.
- **Verification:** Password hashing implementation was reviewed. A separate password-storage penetration test was not performed.
- **Status:** Implementation reviewed.

### Threat 7: Brute-force login attempts

- **Risk:** Attackers may repeatedly guess account passwords.
- **Mitigation:** Authentication logic includes failed-login tracking and account lockout handling.
- **Verification:** Login failure and successful login events were observed in the audit log. A dedicated brute-force test remains pending.
- **Status:** Implementation reviewed; dedicated test pending.

### Threat 8: Audit-log tampering or missing records

- **Risk:** Financial or security events may be modified, deleted, or omitted, making investigation difficult.
- **Mitigation:** The application uses an append-only audit-log design for important events.
- **Verification:** Login, logout, registration, account creation, deposit, transfer, failed payment, and audit-log viewing records were observed.
- **Status:** Event recording tested; tamper-resistance testing pending.

### Threat 9: Invalid or malformed input

- **Risk:** Invalid registration details or transaction values may cause application errors or unexpected behavior.
- **Mitigation:** Request validation is implemented for registration fields, including email, full name, mobile number, and password length.
- **Verification:** Password confirmation mismatch was tested in the frontend. Additional malformed transaction input testing is pending.
- **Status:** Partially tested.

## 3. Security Testing Summary

| Test | Result |
|---|---|
| Customer registration and login | Passed |
| Admin login | Passed |
| Customer account retrieval | Passed |
| Deposit and transfer | Passed |
| Insufficient-funds withdrawal | Passed |
| Duplicate idempotency request | Passed locally |
| Audit-event recording | Passed |
| Direct CUSTOMER access to ADMIN API | Pending |
| Cross-customer account manipulation | Pending |
| Brute-force testing | Pending |
| Audit-log tampering test | Pending |

## 4. Limitations

- Direct administrative API denial has not yet been independently verified using a CUSTOMER token.
- Cross-customer modification attempts require further testing.
- Duplicate transaction protection has been verified locally, not yet in production.
- No formal penetration test or external security audit has been performed.

## 5. Conclusion

LedgerLine implements authentication, role-based authorization, account ownership checks, password hashing, transaction validation, idempotency handling, and audit logging.

The tests and pending items above distinguish verified behavior from protections that still require further testing.