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

| Prefix | Role |
|---|---|
| `/admin/**` | ADMIN |
| `/lecturer/**` | LECTURER |
| `/student/**` | STUDENT |
| `/login`, `/register`, `/forgot-password` | public |

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
