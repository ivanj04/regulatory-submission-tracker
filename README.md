# Regulatory Submission Document Tracker

A production-shaped Spring Boot 3 / Java 17 microservice for managing pharmaceutical regulatory submissions through their full pipeline lifecycle — from initial drafting through FDA/EMA/PMDA approval.

> **Portfolio project** demonstrating the kind of regulatory affairs tooling relevant to **Bristol Myers Squibb's Global Regulatory Sciences** organization. BMS manages concurrent submissions to multiple health authorities worldwide; this system models that exact multi-authority, multi-submission-type workflow.

---

## Why This Matters for BMS Global Regulatory Sciences

| BMS Context | This System |
|---|---|
| IND filings for oncology pipeline compounds | Full IND/NDA/BLA/ANDA/MAA lifecycle tracking |
| Concurrent submissions to FDA + EMA + PMDA | Per-submission `targetAuthority` field + authority-based dashboard filters |
| eCTD document management | S3-backed document storage with presigned upload/download URLs |
| Submission status notifications to global teams | Async email notifications on every status transition |
| Regulatory timeline management | Due date tracking + upcoming deadline dashboard |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 17 + Spring Boot 3.3 |
| Persistence | MongoDB via Spring Data MongoDB |
| Security | Spring Security 6 + JWT (jjwt 0.12) |
| Document Storage | AWS S3 (SDK v2) with LocalStack for local dev |
| Notifications | Spring Mail + `@Async` execution |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Testing | JUnit 5 + Mockito + Flapdoodle embedded MongoDB |
| CI | GitHub Actions + Jenkinsfile |
| Containers | Docker + Docker Compose (MongoDB + LocalStack) |

---

## Submission Pipeline

```
DRAFT
  │
  ├──► IN_PREPARATION
  │         │
  │         ├──► SUBMITTED
  │         │         │
  │         │         └──► UNDER_REVIEW
  │         │                   │
  │         │         ┌─────────┴──────────┐
  │         │         │                    │
  │         │  ADDITIONAL_INFO_REQUESTED  APPROVED
  │         │         │                    │
  │         │         └──► SUBMITTED       └── (terminal)
  │         │
  │         └──► WITHDRAWN (from any non-terminal state)
  └──► WITHDRAWN
```

All transitions are validated server-side. Terminal states (`APPROVED`, `REJECTED`, `WITHDRAWN`) cannot be re-entered. Every transition appends a `StatusHistory` entry and fires an async email notification.

---

## Example API Workflow: DRAFT → SUBMITTED → APPROVED

### 1. Register a user
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"jsmith","email":"jsmith@pharma.com","password":"Pass@1234","role":"SUBMITTER","department":"Regulatory Affairs"}'
# → {"token":"eyJ...","userId":"USR-...","username":"jsmith","role":"SUBMITTER"}
```

### 2. Create a submission
```bash
TOKEN="eyJ..."  # from step 1
curl -X POST http://localhost:8080/api/submissions \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "NDA for Opdivo New Formulation – 505(b)(2)",
    "type": "NDA",
    "sponsor": "Bristol Myers Squibb",
    "targetAuthority": "FDA",
    "dueDate": "2025-06-30"
  }'
# → {"submissionId":"NDA-2024-001","status":"DRAFT",...}
```

### 3. Upload a document (get presigned S3 URL)
```bash
curl -X POST http://localhost:8080/api/submissions/NDA-2024-001/documents \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"fileName":"module5-csr.pdf","fileType":"application/pdf","description":"Module 5 Clinical Study Report","version":1}'
# → {"documentId":"DOC-XXXXXXXX","presignedUploadUrl":"https://s3.amazonaws.com/...","fileKey":"submissions/NDA-2024-001/..."}
# Then PUT the file to the presignedUploadUrl directly from the client
```

### 4. Advance status to SUBMITTED
```bash
curl -X PUT http://localhost:8080/api/submissions/NDA-2024-001/status \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"newStatus":"SUBMITTED","notes":"eCTD package uploaded to FDA ESG"}'
```

### 5. Advance to APPROVED (reviewer role required)
```bash
REVIEWER_TOKEN="eyJ..."  # login as reviewer
curl -X PUT http://localhost:8080/api/submissions/NDA-2024-001/status \
  -H "Authorization: Bearer $REVIEWER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"newStatus":"APPROVED","notes":"FDA approval letter received"}'
```

### 6. View dashboard
```bash
curl http://localhost:8080/api/dashboard \
  -H "Authorization: Bearer $REVIEWER_TOKEN"
# → {"countsByStatus":{...},"countsByType":{...},"countsByAuthority":{...},"upcomingDeadlines":[...]}
```

---

## Running Locally

### Prerequisites
- Java 17+
- Maven 3.9+
- Docker + Docker Compose

### Quick start with Docker Compose
```bash
# Start MongoDB + LocalStack
docker-compose up mongodb localstack -d

# Run the app
mvn spring-boot:run
```

### Environment variables (all optional for local dev)
| Variable | Default | Description |
|---|---|---|
| `MONGO_URI` | `mongodb://localhost:27017/regtracker` | MongoDB connection |
| `JWT_SECRET` | `supersecretkey32charsminimumhere!` | Change in production |
| `AWS_REGION` | `us-east-1` | AWS region |
| `S3_BUCKET` | `reg-submissions-local` | S3 bucket name |
| `MAIL_HOST` | _(empty)_ | SMTP host — if unset, notifications are logged |

### API Documentation
Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## S3 Document Storage Architecture

```
Client                  API Server               AWS S3
  │                        │                       │
  │  POST /documents        │                       │
  ├───────────────────────►│                       │
  │                        │  generatePresignedUrl │
  │                        ├──────────────────────►│
  │                        │◄──────────────────────┤
  │◄───────────────────────┤  presigned PUT URL    │
  │  { presignedUploadUrl } │                      │
  │                        │                       │
  │  PUT file bytes ────────────────────────────► │
  │  (direct to S3,        │                   stored │
  │   no server roundtrip) │                       │
```

The API never proxies file bytes — clients upload directly to S3 using short-lived (15-minute) presigned PUT URLs. Download URLs are valid for 60 minutes. If AWS credentials are not configured, the service returns mock LocalStack URLs so the application starts cleanly.

---

## Running Tests
```bash
mvn test
```

Tests use Flapdoodle embedded MongoDB (no external dependency) and Mockito for service-layer unit tests.

---

## Seeded Test Data

On first startup (non-test profile), three sample submissions are created:

| Submission ID | Type | Authority | Status |
|---|---|---|---|
| `IND-2024-001` | IND | FDA | DRAFT |
| `NDA-2024-001` | NDA | FDA | SUBMITTED |
| `MAA-2024-001` | MAA | EMA | UNDER_REVIEW |

Default credentials: `admin/Admin@1234`, `reviewer/Review@1234`, `submitter/Submit@1234`

---

## Role-Based Access

| Endpoint | SUBMITTER | REVIEWER | ADMIN |
|---|---|---|---|
| `POST /api/submissions` | ✓ | | ✓ |
| `GET /api/submissions/**` | ✓ | ✓ | ✓ |
| `PUT /api/submissions/*/status` | | ✓ | ✓ |
| `GET /api/dashboard` | | ✓ | ✓ |
| `POST /api/submissions/*/documents` | ✓ | ✓ | ✓ |
