# IssueTracker Pro API — Spring Boot 3.3 (Java 17)

Implements the `IssueTracker Pro API v0.2.0` OpenAPI spec: JWT auth, projects + members,
sprints, issues with a forward-only workflow, comments, attachments and notifications.

## Run

```bash
mvn spring-boot:run          # http://localhost:5000/api/v1
```

Swagger UI: http://localhost:5000/swagger-ui.html · H2 console: http://localhost:5000/h2-console

For Postgres, replace the datasource in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/issuetracker
    username: postgres
    password: postgres
```

## Layout

```
domain/      JPA entities (User, Project, ProjectMember, Sprint, Issue, Comment, Attachment, Notification)
repo/        Spring Data repositories (Issue also uses JpaSpecificationExecutor for filtering)
dto/Dtos     Request/response records, field names match the spec exactly (snake_case)
security/    JwtService, JwtAuthFilter, AuthUser principal, @CurrentUser
service/     Business rules (AccessService centralises project permissions)
web/         REST controllers, one per tag
exception/   ApiException + @RestControllerAdvice returning {error, message, status_code}
```

## Functional rules implemented

- **Auth** — unique email (`400 Email already registered, please login`), bcrypt hash, min 6 chars,
  optional domain whitelist (`app.allowed-email-domains`), JWT valid 1 day, demo forgot-password
  returns the new plaintext password. The very first registered user becomes global `admin`.
- **Projects** — key unique, `^[A-Z]{2,10}$`; creator becomes lead + project admin; archived hidden
  unless `include_archived=true`; new lead must already be a member; delete cascades to sprints,
  issues, comments and attachments (lead or admin only).
- **Members** — duplicate add → 400; last lead cannot be removed or demoted; removing a member
  unassigns their issues in that project.
- **Sprints** — name unique per project, `end_date >= start_date`, created inactive; activating
  deactivates the other active sprint and rejects a future `start_date`; delete blocked when the
  sprint is active (400) or contains issues (409); complete moves unfinished issues to backlog
  with `sprint_id = null`.
- **Issues** — `reporter_id` forced to the caller; assignee must be a project member; parent must be
  in the same project and not itself a subtask; sprint must be active on create; defaults
  `status=backlog`, `priority=medium`; edit allowed for assignee/reporter/lead/admin; delete for
  lead/admin only and 409 when subtasks exist.
- **Transitions** — `backlog→todo→in_progress→in_review→done`, no backwards moves; `GET` returns the
  allowed set for the caller (empty when not permitted).
- **Comments** — member role required, 1–5000 chars, edit only by author, delete by author/lead/admin,
  `@email` mentions raise notifications.
- **Attachments** — max 10 MB, `image/*`, `application/pdf`, `text/plain`; stored under
  `app.uploads.dir` and served from `/api/v1/files/**`; `GET /attachments/{id}` returns a 302 redirect;
  delete by uploader/lead/admin.
- **Notifications** — created on assignment, mention and status change; list supports `unread_only`
  and pagination; only the owner can mark one read.

## Notes

- `viewer` project role is read-only; every write path goes through `AccessService.requireWrite`.
- Change `app.jwt.secret` before any non-local use.
- `/api/v1/files/**` is behind auth; if you want browser-follow of the 302 redirect without a token,
  permit that path in `SecurityConfig`.
