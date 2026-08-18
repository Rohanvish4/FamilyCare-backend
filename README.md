# FamilyCare AI - Identity & Authentication Service

Identity, authentication, authorization, and audit logging module for the FamilyCare AI platform. Implemented as a Spring Boot 3.3+ modular monolith following Clean Layered Architecture and Domain-Driven Design (DDD) principles.

---

## 1. Tech Stack & Infrastructure

| Component | Technology | Version | Purpose |
|---|---|---|---|
| Runtime | Java | 21 (LTS) | Execution environment |
| Framework | Spring Boot | 3.3.5 | Application framework (Web, Data JPA, Security, Validation) |
| Security | Spring Security | 6.x | Security filter chain & RBAC |
| JWT Engine | JJWT | 0.12.6 | Access token issue/verify (`io.jsonwebtoken:jjwt-api`) |
| Password Hashing | BCrypt | Cost 12 | Password storage encryption |
| Database | PostgreSQL | 16+ | Relational data store |
| Migration | Flyway | 10.x | Database schema versioning |
| Object Mapping | MapStruct / Lombok | 1.5.5 / 1.18.34 | Type-safe DTO/Entity compilation mapping |
| API Specification | SpringDoc OpenAPI | 2.6.0 | Swagger UI and OpenAPI 3.0 schema generation |

---

## 2. Local Setup & Execution

### Step 1: Start PostgreSQL Container
Launch the PostgreSQL 16 container using Docker Compose:
```bash
docker compose up -d
```

### Step 2: Configure Environment Variables
Copy the template file to `.env` and adjust database/JWT values if needed:
```bash
cp .env.example .env
```

### Step 3: Run Tests
Execute the unit and integration test suite:
```bash
./mvnw test
```
*(On Windows PowerShell, use `.\mvnw.cmd test`)*

### Step 4: Launch Application
Start the backend server locally:
```bash
./mvnw spring-boot:run
```
The application starts on port `8080` by default.

---

## 3. Security Architecture & Token Lifecycle

### Refresh Token Rotation (RTR - RFC 6819)
- **Plaintext Exposure**: Plaintext refresh tokens are returned to the client once upon issuance.
- **SHA-256 Persistence**: Only the SHA-256 hash (`token_hash`) of the refresh token is stored in the database.
- **Single-Use Rotation**: Exchanging a refresh token revokes the previous token, sets `replaced_by_token_hash`, and issues a new access token + refresh token pair.
- **Token Reuse Detection**: If an already-revoked refresh token is presented, the system detects a security breach, revokes all active refresh tokens associated with that user account, and returns HTTP 401 with code `ERR_TOKEN_REUSE_DETECTED`.

### Account Lockout Policy
- Tracks consecutive failed password attempts per email address.
- 5 consecutive failed attempts trigger an automatic 15-minute lockout (`lockout_until`).
- Attempts made during the lockout period return HTTP 401 / 423 with time remaining.
- Successful authentication resets the counter to 0.

### Clinical Role Verification
- Accounts registered with patient or family roles (`ROLE_PATIENT`, `ROLE_FAMILY_MEMBER`) are initialized with `ACTIVE` status.
- Accounts registered with medical roles (`ROLE_DOCTOR`, `ROLE_PHARMACIST`, `ROLE_LAB_TECHNICIAN`) are initialized with `PENDING_VERIFICATION` status, requiring administrative verification before full resource access.

---

## 4. API Endpoints Overview

| Method | Path | Auth Required | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Register a new user account |
| `POST` | `/api/v1/auth/login` | Public | Authenticate user & issue JWT token pair |
| `POST` | `/api/v1/auth/refresh` | Public | Rotate refresh token & issue new JWT pair |
| `POST` | `/api/v1/auth/logout` | Bearer Token | Revoke active refresh token session |
| `GET` | `/api/v1/users/me` | Bearer Token | Fetch authenticated user profile details |
| `GET` | `/api/v1/health` | Public | Service health & operational status |

---

## 5. OpenAPI & Swagger Documentation

Interactive OpenAPI 3.0 documentation is available when the service is running:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
