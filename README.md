# Titan SIS: Student Information System

CPSC 362 group project. A web app for course registration, class rolls, grades,
transcripts and graduation checks, with separate portals for students, professors
and the registrar's office.

Java 17+ and Spring Boot 4 on the server, plain HTML/CSS/JavaScript in the browser,
and an embedded H2 database. The repository includes the Maven wrapper, so a JDK is
the only thing you need to install.

## Run it

```bash
./mvnw spring-boot:run          # macOS / Linux
.\mvnw.cmd spring-boot:run      # Windows (PowerShell or Command Prompt)
```

On Windows, type `.\mvnw.cmd` wherever this README says `./mvnw`. PowerShell only runs a
script from the current folder when its name starts with `.\`.

Open http://localhost:8080 and sign in with a demo account. The database lives in
memory and is rebuilt with demo data every time the app starts.

To build a single runnable jar instead:

```bash
./mvnw package
java -jar target/sis.jar
```

### Demo accounts

Every demo account uses the password `Titans-2026`.

| Username    | Role          | Who                                                                     |
|-------------|---------------|-------------------------------------------------------------------------|
| `registrar` | Administrator | Registrar's Office                                                      |
| `akim`      | Professor     | Dr. Alicia Kim, Computer Science (CPSC 362, 351, 491, 335, ...)        |
| `bcarter`   | Professor     | Dr. Ben Carter, Computer Science                                        |
| `praman`    | Professor     | Dr. Priya Raman, Mathematics                                            |
| `mlopez`    | Student       | Maria Lopez, CWID 885104273, junior with 64 units (the object diagram) |
| `ebrooks`   | Student       | Ethan Brooks, CWID 885019382, senior who only needs CPSC 491            |
| `sortiz`    | Student       | Sam Ortiz, CWID 885331206, first-year student taking CPSC 131           |

The demo data recreates the team's object diagram: Maria passed CPSC 131 with an A- in
section 02 of Spring 2026 (40 of 40 seats) and holds one of the 38 taken seats in
Dr. Kim's CPSC 362 section 01 in Fall 2026. Fall 2026 is in progress and Spring 2027
is open for registration. Course descriptions in the demo catalog are short
placeholders written for this project, not official catalog text.

### A two-minute demo

1. Sign in as `mlopez`. On **Register**, try CPSC 471: it is refused because CPSC 351
   is not finished yet. Enroll in CPSC 323, then open **My schedule** to see the week.
2. Sign in as `akim`. Open the roll for CPSC 491 (Fall 2026) and give Ethan Brooks an A.
3. Sign in as `registrar`. Under **Transcripts and graduation**, run the graduation
   check for 885019382: Ethan is now eligible. Generate Maria's transcript and print it.
4. In any portal, use **Ask the assistant**: "What do I need before CPSC 362?"

### Pretend it is a different day

Adding and dropping depend on today's date (they close after the first week of the
term). To demo a specific day, pin the date:

```bash
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--sis.today=2027-01-29"
java -jar target/sis.jar --sis.today=2027-01-29
```

Keep the quotes around the `-D` option. Without them, Windows PowerShell splits it at
the first dot and Maven stops with "Unknown lifecycle phase".

## Tests

```bash
./mvnw test     # unit tests only (fast)
./mvnw verify   # unit + integration + behavioral tests, then builds target/sis.jar
```

| Suite       | What it covers                                                           | Where                                                  |
|-------------|--------------------------------------------------------------------------|--------------------------------------------------------|
| Unit        | Domain rules: grade levels, GPA, add/drop dates, seats, time conflicts, prerequisites, graduation | `src/test/java/edu/csuf/sis/domain` (`*Test`) |
| Integration | The REST API through Spring Security: sign-in, role access, every portal's endpoints, the demo data | `src/test/java/edu/csuf/sis/gateway` (`*IT`) |
| Behavioral  | Cucumber scenarios for each use case, run against the real components     | `src/test/resources/features`                          |

Reports: `target/surefire-reports` (unit), `target/failsafe-reports` (integration),
and `target/cucumber-report.html` (behavioral scenarios, readable in a browser).

Tests use real components and an in-memory database; there are no mocks. A test clock
lets scenarios set "today", so date rules are tested without waiting for real dates.
The browser portals are not covered by automated tests; they were checked by hand
in a browser.

## How the code maps to the design

### Component diagram

| Component (interface)                              | Code                                                                 |
|----------------------------------------------------|----------------------------------------------------------------------|
| Web Client                                         | `src/main/resources/static`: `index.html`, `student.html`, `professor.html`, `admin.html`, `js/`, `css/` |
| API Gateway (SIS REST API)                         | `gateway`: `SecurityConfig`, `SessionTokenFilter`, the `*Controller` classes, `ApiErrorHandler` |
| Course Catalog & Scheduling (`ICatalog`)           | `catalog`: `ICatalog`, `CatalogService`                              |
| Registration (`IRegistration`)                     | `registration`: `IRegistration`, `RegistrationService`               |
| Academic Records (`IRecords`)                      | `records`: `IRecords`, `RecordsService`                              |
| Degree Audit (`IDegreeAudit`)                      | `audit`: `IDegreeAudit`, `DegreeAuditService`                        |
| Identity & Access (`IAuth`, `IUserAdmin`)          | `identity`: `IAuth`/`AuthService`, `IUserAdmin`/`UserAdminService`   |
| Chatbot Assistant (`IChat`)                        | `chat`: `IChat`, `ChatbotAssistant`                                  |
| SIS Database                                       | `domain`: JPA entities and Spring Data repositories, on H2           |

Dependencies follow the diagram's arrows: `RegistrationService` uses only `ICatalog`
and `IRecords`, `DegreeAuditService` reads grades through `IRecords`, and
`ChatbotAssistant` uses only `ICatalog`. Controllers call component interfaces, never
repositories. The diagram's external LLM Provider is not wired in: the assistant is
rule-based and answers from catalog data only. A different `IChat` implementation could
call a language model without changing anything else.

### Data stores (Level 1 DFD)

| Data store              | Entities                                         |
|-------------------------|--------------------------------------------------|
| D1 User Accounts        | `UserAccount`, `Student`, `Professor`, `UserSession` |
| D2 Course Catalog       | `Course` (with its prerequisites)                |
| D3 Class Schedule       | `Term`, `Section` (keeps its own seat count)     |
| D4 Enrollments & Grades | `Enrollment`                                     |
| D5 Degree Requirements  | `Major`                                          |

### Use cases and their tests

| Use case                                          | Tested in                                         |
|---------------------------------------------------|---------------------------------------------------|
| Log In                                            | `accounts.feature`, `AuthApiIT`                   |
| Browse Sections & Openings                        | `StudentApiIT`, `chatbot.feature`                 |
| Register for Class Section, Check Prerequisites   | `registration.feature`, `StudentApiIT`            |
| Drop Class                                        | `drop.feature`                                    |
| View Student Schedule, View Academic History      | `transcripts.feature`, `ProfessorApiIT`           |
| View Class Roll, Drop Student from Class          | `grades.feature`, `drop.feature`, `ProfessorApiIT` |
| Enter Grades, Update Grade Level                  | `grades.feature`                                  |
| Add Course & Prerequisites, Enter Semester Class Schedule | `AdminApiIT`                              |
| Define Major Requirements, Run Graduation Check   | `graduation.feature`, `AdminApiIT`                |
| Add Student / Professor Account & Password, Revoke User Authorization | `accounts.feature`, `AdminApiIT` |
| Generate Transcript                               | `transcripts.feature`, `AdminApiIT`               |

## Business rules

- **Prerequisites:** every prerequisite must be passed (D- or better) before registering.
- **Registration:** one section of a course per term, no time conflicts with the
  student's other classes, no retaking a course already passed, and never more
  students than seats. Seat counting locks the section row, so two students cannot
  take the last seat at the same time.
- **Add/drop:** students can add or drop through the last day of the first week of
  classes (start date + 6 days). After that only the professor can drop a student.
- **Grades:** A+ through F. Posting a grade completes the class; an F counts in the
  GPA but earns no units.
- **Grade level:** freshman 0-29, sophomore 30-59, junior 60-89, senior 90+ units
  passed. Updated every time grades are posted.
- **Graduation:** every course the major requires, plus the major's total units.
- **Revoking access:** disables the login and ends its sessions; enrollments, grades and
  transcripts stay. The registrar cannot revoke their own access.
- **Professors** can only see rolls, post grades and drop students in sections they
  teach. They can look up any student's schedule and history.

## Security

- Passwords are hashed with BCrypt. Signing in returns a random 256-bit session token;
  only its SHA-256 hash is stored, and it expires after 8 hours.
- Sessions live in the database, not in server memory, so several app servers behind a
  load balancer can share them.
- Every `/api/**` call needs `Authorization: Bearer <token>`, and each role can reach
  only its own portal's API (`/api/student`, `/api/professor`, `/api/admin`).
- Pages send a strict Content-Security-Policy (scripts, styles and data from this server
  only) and refuse to be framed. The browser keeps the token in `sessionStorage`, so
  closing the tab signs out.
- Errors use standard problem responses (`application/problem+json`); refusals explain
  what rule was broken without leaking internals.

## Configuration

Set these in `src/main/resources/application.properties` or on the command line
(`--name=value`):

| Property                 | Default | Meaning                                                  |
|--------------------------|---------|----------------------------------------------------------|
| `sis.seed.enabled`       | `true`  | Load the demo data on startup. Turn off for real data.   |
| `sis.today`              | blank   | Pretend today is this date (`yyyy-MM-dd`).               |
| `sis.auth.session-hours` | `8`     | How long a sign-in lasts.                                |

To keep data between restarts, store H2 in a file:

```properties
spring.datasource.url=jdbc:h2:file:./data/sis
spring.jpa.hibernate.ddl-auto=update
```

For a hosted database such as Azure SQL or PostgreSQL, add its JDBC driver to
`pom.xml` and set `spring.datasource.url`, `username` and `password`.

## REST API

All responses are JSON. Every endpoint except sign-in needs a bearer token.

| Method & path                                         | Who        | What                                  |
|-------------------------------------------------------|------------|---------------------------------------|
| `POST /api/auth/login`                                | anyone     | Sign in; returns a token              |
| `POST /api/auth/logout`, `GET /api/auth/me`           | signed in  | Sign out; who am I                    |
| `GET /api/catalog/terms`, `/courses`                  | signed in  | Terms and courses                     |
| `GET /api/catalog/terms/{id}/sections?search=`        | signed in  | Sections with open seats              |
| `GET /api/student/profile`, `/schedule`, `/history`   | student    | Own record                            |
| `POST /api/student/enrollments` `{sectionId}`         | student    | Register                              |
| `DELETE /api/student/enrollments/{sectionId}`         | student    | Drop                                  |
| `GET /api/professor/sections`, `/sections/{id}/roll`  | professor  | Own sections and rolls                |
| `PUT /api/professor/sections/{id}/grades`             | professor  | Post grades `{grades:[{studentId, grade}]}` |
| `DELETE /api/professor/sections/{id}/students/{sid}`  | professor  | Drop a student                        |
| `GET /api/professor/students/{cwid}[/schedule\|/history]` | professor | Look up a student                  |
| `GET /api/admin/accounts`, `POST /api/admin/students`, `POST /api/admin/professors` | admin | Accounts |
| `POST /api/admin/accounts/{id}/revoke`, `/restore`; `PUT .../password` | admin | Access and passwords      |
| `POST /api/admin/courses`, `/terms`, `/sections`      | admin      | Catalog and class schedule            |
| `GET /api/admin/majors`, `PUT /api/admin/majors/{code}` | admin    | Degree requirements                   |
| `GET /api/admin/students/{cwid}/transcript`, `/graduation-check` | admin | Transcripts, graduation checks |
| `POST /api/chat` `{message}`                          | signed in  | Ask the assistant                     |

## CI/CD

`.github/workflows/ci.yml` runs on every push and pull request: build, unit tests,
then integration and behavioral tests, then it publishes the test reports and
`sis.jar` as build artifacts. A deployment stage would take that jar.
