# Employee Leave Management System – REST API

Spring Boot 3 REST API where employees apply for leave and their managers approve or reject it.
JWT authentication, role-based access, MySQL (or PostgreSQL / H2), layered architecture
(Controller → Service → Repository), DTOs only (entities are never exposed).

## Tech stack

Java 17+ · Spring Boot 3.3 · Spring Web · Spring Data JPA · Spring Security + JWT (jjwt 0.12) ·
MySQL 8 (H2 profile for quick runs) · Bean Validation · Maven · JUnit 5 + Mockito

## Project structure

```
src/main/java/com/example/leave
├── controller/   AuthController, LeaveController          (HTTP only, no business logic)
├── service/      AuthService, LeaveService                (all business rules)
├── repository/   EmployeeRepository, LeaveRequestRepository
├── entity/       Employee, LeaveRequest, Role, LeaveType, LeaveStatus
├── dto/          Request/Response records, ErrorResponse
├── validation/   @ValidDateRange custom validator
├── exception/    Custom exceptions + GlobalExceptionHandler (@RestControllerAdvice)
├── security/     JwtService, JwtAuthenticationFilter, SecurityConfig, UserDetails...
└── config/       ClockConfig, ManagerSeeder
src/main/resources/application.yml        (externalized DB / JWT config)
src/main/resources/application-h2.yml     (in-memory profile)
postman/                                   (Postman collection + .http file)
docker-compose.yml                         (MySQL for local dev)
```

## Setup

### Prerequisites
- JDK 17 or newer
- Maven 3.9+
- MySQL 8 (or Docker) – or use the `h2` profile for a zero-setup run

### 1. Start the database (MySQL via Docker)
```bash
docker compose up -d
```
This starts MySQL on port 3306 with database `leave_db` and root password `root`.
Without Docker, create the database yourself: `CREATE DATABASE leave_db;`

### 2. Configure (optional)
All settings are in `src/main/resources/application.yml` and can be overridden with environment variables:

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/leave_db?...` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `root` | DB credentials |
| `DB_DRIVER` | `com.mysql.cj.jdbc.Driver` | JDBC driver |
| `DDL_AUTO` | `update` | Hibernate schema mode (use `validate` in production) |
| `JWT_SECRET` | dev placeholder | **Change it.** Min. 32 characters |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Token lifetime |
| `SEED_MANAGER_EMAIL` / `SEED_MANAGER_PASSWORD` | `manager@company.com` / `Manager@123` | Default manager account |
| `SERVER_PORT` | `8080` | HTTP port |

**PostgreSQL:** add the `org.postgresql:postgresql` runtime dependency and set
`DB_URL=jdbc:postgresql://localhost:5432/leave_db`, `DB_DRIVER=org.postgresql.Driver`.

### 3. Run
```bash
mvn spring-boot:run
# or without MySQL:
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
The API is available at `http://localhost:8080`.

### 4. Run the tests
```bash
mvn test
```

## Built-in web UI

Once the app is running, open **http://localhost:8080** in a browser. It is one static page
(`src/main/resources/static/index.html`, plain HTML/CSS/JS, no build step) that talks to the same REST API.

- **Login / Register** tabs (new accounts are employees; Manager ID 1 is the seeded manager)
- **Employee view:** remaining balance, apply-for-leave form (shows the number of days), and leave history with
  PENDING / APPROVED / REJECTED badges and the manager's comment
- **Manager view:** team requests with a status filter and **Approve / Reject** buttons (reject asks for an optional comment)
- Pages refresh automatically every 10 seconds, so an employee sees an approval without reloading
- Server errors (past date, insufficient balance, wrong manager...) are shown as messages on the page
- The page itself is public, but every data call still needs the JWT, so the API stays secured

## The default manager

`POST /api/auth/register` always creates an `EMPLOYEE` (the role cannot be chosen by the caller –
otherwise anyone could make themselves a manager). To have someone who can approve requests,
a manager account is **seeded on first startup**:

- email: `manager@company.com`, password: `Manager@123` (change via env vars; disable with `SEED_MANAGER_ENABLED=false`)

Register employees with `"managerId": <that manager's id>` (usually `1`) so the manager can see and
approve their requests. Login returns `employeeId`, so you can read the manager's id from the manager login response.

## Web UI

A small built-in web page is served by Spring Boot itself (no separate frontend server, no Node.js needed).
Start the app and open **http://localhost:8080** in a browser:

- **Login / Register** (the seeded manager can be filled in with one click)
- **Employee:** leave balance, apply-for-leave form (shows the number of days before you submit), and your request history with status (PENDING / APPROVED / REJECTED) and the manager's comment
- **Manager:** counts of pending/approved/rejected requests, filterable team table, Approve and Reject (with optional comment) buttons

The page refreshes itself every 10 seconds, so an employee sees the manager's decision without reloading.
The page is a single file: `src/main/resources/static/index.html`. It calls the same REST API as everything
else, so all JWT and role rules still apply (only `GET /` and `GET /index.html` are public).

## Endpoints

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/auth/register` | public | Register an employee |
| POST | `/api/auth/login` | public | Returns a JWT |
| POST | `/api/leaves` | EMPLOYEE | Apply for leave |
| GET | `/api/leaves/my-history` | EMPLOYEE | Own leave requests |
| GET | `/api/leaves/balance` | EMPLOYEE | Remaining leave balance |
| GET | `/api/leaves/team` | MANAGER | Leave requests of employees reporting to me |
| PUT | `/api/leaves/{id}/approve` | MANAGER | Approve (deducts balance) |
| PUT | `/api/leaves/{id}/reject` | MANAGER | Reject, optional `{ "comment": "..." }` body |

Send the token on every protected call: `Authorization: Bearer <token>`.

## Business rules

- Start date cannot be in the past (today is allowed); end date cannot be before start date.
- `numberOfDays` is calculated by the server: **inclusive calendar days** (Mon–Wed = 3, one day = 1).
  Weekends/holidays are not excluded – adapt `LeaveService.calculateNumberOfDays` if you need that.
- Applying fails (400) if the requested days exceed the remaining balance. Default balance: 12 days.
- A new request cannot overlap another PENDING/APPROVED request of the same employee (409).
- Only the employee's **own** manager can approve/reject (403 otherwise).
- Only PENDING requests can be reviewed (409 if already approved/rejected – prevents double deduction).
- Approval deducts the days from `annualLeaveBalance` (balance is re-checked at approval time, and
  `Employee` uses optimistic locking so concurrent approvals cannot over-deduct).
- Rejection never changes the balance.
- Role-based access follows the spec: apply/history/balance are `EMPLOYEE` only; team/approve/reject are `MANAGER` only.

## Sample requests

**Register**
```bash
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{
  "name": "Asha", "email": "asha@company.com", "password": "Secret@123",
  "department": "Engineering", "managerId": 1
}'
```
```json
{ "id": 2, "name": "Asha", "email": "asha@company.com", "role": "EMPLOYEE",
  "department": "Engineering", "managerId": 1, "annualLeaveBalance": 12 }
```

**Login**
```bash
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{ "email": "asha@company.com", "password": "Secret@123" }'
```
```json
{ "token": "eyJhbGciOi...", "tokenType": "Bearer", "employeeId": 2, "email": "asha@company.com", "role": "EMPLOYEE" }
```

**Apply for leave**
```bash
curl -X POST http://localhost:8080/api/leaves \
  -H "Authorization: Bearer $EMP_TOKEN" -H "Content-Type: application/json" -d '{
  "leaveType": "CASUAL", "startDate": "2027-01-12", "endDate": "2027-01-14", "reason": "Family function"
}'
```
```json
{ "id": 1, "employeeId": 2, "employeeName": "Asha", "leaveType": "CASUAL",
  "startDate": "2027-01-12", "endDate": "2027-01-14", "numberOfDays": 3, "status": "PENDING",
  "reason": "Family function", "appliedOn": "2026-10-09", "reviewedBy": null, "managerComment": null }
```

**Balance / history**
```bash
curl http://localhost:8080/api/leaves/balance -H "Authorization: Bearer $EMP_TOKEN"
curl http://localhost:8080/api/leaves/my-history -H "Authorization: Bearer $EMP_TOKEN"
```

**Manager: team requests, approve, reject**
```bash
curl http://localhost:8080/api/leaves/team -H "Authorization: Bearer $MGR_TOKEN"
curl -X PUT http://localhost:8080/api/leaves/1/approve -H "Authorization: Bearer $MGR_TOKEN"
curl -X PUT http://localhost:8080/api/leaves/2/reject -H "Authorization: Bearer $MGR_TOKEN" \
  -H "Content-Type: application/json" -d '{ "comment": "Release week - please reschedule" }'
```

## Windows PowerShell guide (run + demo data)

Everything below uses plain PowerShell (`Invoke-RestMethod`), so no extra tools are needed.

### Start the app

1. Open the project folder (the one containing `pom.xml`) in VS Code and open a terminal.
2. Run:
   ```powershell
   mvn spring-boot:run "-Dspring-boot.run.profiles=h2"
   ```
3. Wait for `Started LeaveManagementApplication`. **Keep this terminal open** – it is the running server. Stop it with `Ctrl+C`.

If `mvn` is not recognized in a new terminal, add Maven to the PATH for that session
(adjust the folder if Maven is installed elsewhere):
```powershell
$env:Path += ";$env:USERPROFILE\apache-maven-3.9.11\bin"
```

> The `h2` profile is an in-memory database: **all data is cleared on every restart**. Only the seeded
> manager (id 1) is re-created automatically. Register employees again after a restart.
> For persistent data use MySQL (see Setup above).

### Send commands (open a second terminal with the `+` button)

**Setup block** – run once in every new terminal:
```powershell
$base = "http://localhost:8080"; $json = "application/json"
$mgr = (Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType $json -Body '{"email":"manager@company.com","password":"Manager@123"}').token
$m = @{ Authorization = "Bearer $mgr" }
```

**Register an employee and log in:**
```powershell
Invoke-RestMethod -Method Post -Uri "$base/api/auth/register" -ContentType $json -Body '{"name":"Asha","email":"asha@company.com","password":"Secret@123","department":"Engineering","managerId":1}'
$emp = (Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType $json -Body '{"email":"asha@company.com","password":"Secret@123"}').token
$e = @{ Authorization = "Bearer $emp" }
```

**Apply for leave** (3 days, starting 30 days from today – change `30` to avoid overlapping an earlier request):
```powershell
$s  = (Get-Date).AddDays(30).ToString("yyyy-MM-dd")
$en = (Get-Date).AddDays(32).ToString("yyyy-MM-dd")
$body = @{ leaveType = "CASUAL"; startDate = $s; endDate = $en; reason = "Family function" } | ConvertTo-Json
$leave = Invoke-RestMethod -Method Post -Uri "$base/api/leaves" -Headers $e -ContentType $json -Body $body
$leave
```

**Manager approves or rejects** (only PENDING requests; apply again to get a new pending one):
```powershell
# approve (deducts the days from the balance)
Invoke-RestMethod -Method Put -Uri "$base/api/leaves/$($leave.id)/approve" -Headers $m

# or reject with a comment (no deduction)
Invoke-RestMethod -Method Put -Uri "$base/api/leaves/$($leave.id)/reject" -Headers $m -ContentType $json -Body '{"comment":"Release week"}'
```

### View the data

```powershell
# Employee: remaining balance and own history
Invoke-RestMethod -Uri "$base/api/leaves/balance" -Headers $e
Invoke-RestMethod -Uri "$base/api/leaves/my-history" -Headers $e | Format-Table id,leaveType,startDate,endDate,numberOfDays,status

# Manager: all requests from the team
Invoke-RestMethod -Uri "$base/api/leaves/team" -Headers $m | Format-Table id,employeeName,leaveType,startDate,numberOfDays,status,managerComment
```
Add `| ConvertTo-Json` to any command to see the raw JSON.

### Check that security works

```powershell
Invoke-RestMethod -Uri "$base/api/leaves/team" -Headers $e      # employee on a manager endpoint -> 403
Invoke-RestMethod -Uri "$base/api/leaves/balance"               # no token -> 401
```

### Java 25 note

On JDK 24+ the `pom.xml` passes `-Dnet.bytebuddy.experimental=true` to the tests and to `spring-boot:run`
(Mockito and Hibernate use Byte Buddy, which does not officially support the newest JDKs yet).
It is harmless on JDK 17/21. If you run the packaged jar directly, add the same flag:
`java -Dnet.bytebuddy.experimental=true -jar target/leave-management-1.0.0.jar`.

## Error format

Every error uses the same JSON shape:

```json
{
  "timestamp": "2026-10-09T03:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Insufficient leave balance: requested 5 day(s) but only 2 day(s) available",
  "path": "/api/leaves"
}
```
Validation failures add a `validationErrors` map (`{"startDate": "startDate cannot be in the past"}`).

| Status | When |
|---|---|
| 400 | Validation errors, malformed JSON / bad enum, past start date, insufficient balance |
| 401 | Missing/invalid/expired token, wrong login credentials |
| 403 | Wrong role, or a manager acting on someone else's team |
| 404 | Leave request / employee / manager not found |
| 409 | Duplicate email, overlapping leave, request already reviewed, concurrent update |

## Postman / .http

- `postman/Leave-Management.postman_collection.json` – import into Postman. Run folders in order
  (Auth → Employee → Manager); tokens and the leave id are stored in collection variables by test scripts,
  and include negative cases (401/403/404/409/400).
- `postman/leave-management.http` – same flow for the VS Code REST Client extension.

## Tests

`mvn test` runs (JUnit 5 + Mockito, no database needed):

- `LeaveServiceTest` – day/balance calculation, insufficient balance rejection, past start date rejection,
  approval deducts balance, rejection does not deduct, wrong manager cannot approve/reject, overlap, double approval, team view
- `AuthServiceTest` – password hashing, default role/balance, duplicate email, non-manager `managerId`
- `JwtServiceTest` – token generation/validation, forged/expired tokens, short secret
- `LeaveApplyRequestValidationTest` – Bean Validation incl. the custom `@ValidDateRange`

## Production notes

- Set a strong `JWT_SECRET` and use `DDL_AUTO=validate` with Flyway/Liquibase migrations.
- Change or disable the seeded manager credentials.
- Consider adding refresh tokens, rate limiting on `/api/auth/login`, and pagination on list endpoints.
