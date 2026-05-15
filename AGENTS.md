# AGENTS.md

## Stack
- Spring Boot 4.0.3 (Jakarta EE — `jakarta.*`, not `javax.*`), Java 17, Maven wrapper (`./mvnw`)
- Thymeleaf templates, Spring Security (form + OAuth2), Spring Data JPA, PostgreSQL
- File uploads stored in `uploads/` (configurable via `app.upload.dir`)

## Setup (required before running)

**`application.properties` is gitignored** — a fresh clone will not have one. You must create
`src/main/resources/application.properties`. The local copy (present on disk but never committed)
is the reference. Key values you need to provide:
```
spring.datasource.url=jdbc:postgresql://localhost:5432/asp_db
spring.datasource.username=postgres
spring.datasource.password=<your-pg-pass>
```
PostgreSQL must be running on `localhost:5432`.

## Exact commands

| Action | Command |
|---|---|
| Run dev server | `./mvnw spring-boot:run` |
| Run all tests | `./mvnw test` |
| Package JAR | `./mvnw clean package` |

No linter, formatter, or typecheck config exists.

## Key facts

- **Entrypoint**: `com.finalyearproject.FinalyearprojectApplication` (`src/main/java/.../FinalyearprojectApplication.java`)
- **Default admin** (seeded by `DataInitializer`): `admin@university.com` / `admin123`
- **Roles**: `ADMIN`, `LECTURER`, `STUDENT` — defined in `Role.RoleType` enum
- **Students auto-approved** on registration; **lecturers must be created by admin** (registration rejects lecturer signup)
- **Login accepts** either email or `universityId` (handled by `CustomUserDetailsService`)
- **Student ID format**: `dd/XX/BU/R/d+` (e.g. `23/BS/BU/R/12345`)
- **Post-login redirect**: `/admin/dashboard`, `/lecturer/dashboard`, or `/student/dashboard` based on role
- **OAuth2 login** redirects all users to `/student/dashboard`
- **Remember-me key**: `bugema-remember-me`, valid 7 days
- **Password**: BCrypt via `PasswordEncoderConfig` -> `BCryptPasswordEncoder`

## Routes

| Prefix / Path | Role |
|---|---|
| `/admin/**` | ADMIN |
| `/lecturer/**` | LECTURER |
| `/student/**` | STUDENT |
| `/login`, `/register`, `/forgot-password`, `/reset-password` | public |
| `/` | redirects to `/login` |

## Password reset flow

1. User enters email on `POST /forgot-password` → generates UUID token (stored in-memory `PasswordResetTokenService`, 1hr expiry) → sends email with `http://host:port/reset-password?token=...` link
2. `GET /reset-password?token=...` validates token → renders `reset-password.html`
3. `POST /reset-password` validates token, checks passwords match (min 6 chars) → updates password via `UserService.updatePassword()` → redirects to login
- **CSRF is disabled** (`csrf.disable()`) — forms need no CSRF token
- **Admin-initiated reset**: `POST /admin/users/{id}/reset-password` generates temp password and emails it via `UserService.sendPasswordResetEmail()`

## Message patterns (all 3 roles)

Each role has the same message endpoints (replace `{role}` with `admin`, `lecturer`, or `student`):

| Endpoint | Template |
|---|---|
| `GET /{role}/messages` | `{role}-messages` (sidebar + empty chat) |
| `GET /{role}/messages/{userId}` | `{role}-conversation` (sidebar + active chat) |
| `POST /{role}/messages/send` | redirects to conversation |
| `GET /{role}/messages/{id}/refresh` | returns `fragments/message-list :: messages` |
| `POST /{role}/messages/{id}/read` | marks conversation read |
| `POST /{role}/messages/typing` | SSE typing indicator |

Key model attributes all 3 message pages load: `conversations`, `onlineUserIds`, `unreadPerUser`, `allUsersExceptCurrent`, `other`, `isOtherOnline`.

## Error handling

- Custom error pages: `error/404.html`, `error/500.html`
- `GlobalExceptionHandler` in `config/` catches unhandled exceptions
- `server.error.whitelabel.enabled=false` (in `application.properties`)
- Stack traces are NOT exposed to users (`include-stacktrace` removed from properties)

## Testing

- Single test class: `FinalyearprojectApplicationTests` (context load smoke test)
- No test-specific config — inherits from `application.properties`, so needs a running PG database

## Package layout

```
com.finalyearproject
├── config/          SecurityConfig, WebConfig, DataInitializer, etc.
├── controller/      AuthController, AdminController, LecturerController, StudentController, etc.
├── model/           User, Course, Assignment, Submission, Material, etc.
├── repository/      JPA repositories
└── service/         Business logic, FileStorageService, EmailService, etc.
```
