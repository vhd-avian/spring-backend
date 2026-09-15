# IssueTracker Pro API — Spring Boot 3.3 (Java 17)

Implements the `IssueTracker Pro API v0.2.0` OpenAPI spec: JWT auth, projects + members,
sprints, issues with a forward-only workflow, comments, attachments and notifications.

## Local Development

### Prerequisites
- Java 17
- Maven 3.9+

### Run
```bash
mvn spring-boot:run
```
- API Root: `http://localhost:5002/api/v1`
- Swagger UI: `http://localhost:5002/swagger-ui.html`
- H2 Console: `http://localhost:5002/h2-console`

## Production Deployment

### Environment Variables
The following environment variables can be used to configure the application:

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Server port | `5002` |
| `SPRING_DATASOURCE_URL` | JDBC URL (e.g. `jdbc:postgresql://db:5432/issuetracker`) | H2 Memory DB |
| `SPRING_DATASOURCE_USERNAME` | DB Username | `sa` |
| `SPRING_DATASOURCE_PASSWORD` | DB Password | (empty) |
| `JWT_SECRET` | Secret for JWT signing (must be strong/long) | (default provided) |
| `JWT_EXPIRATION` | JWT expiration in ms | `86400000` (1 day) |
| `UPLOAD_DIR` | Directory for file uploads | `./uploads` |
| `ALLOWED_EMAIL_DOMAINS` | Comma-separated list of allowed domains | (empty = disable) |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed CORS origins | `*` |
| `SPRING_H2_CONSOLE_ENABLED` | Enable H2 Console (set to `false` in prod) | `true` |
| `SPRING_JPA_DDL_AUTO` | Hibernate DDL strategy (`update`, `validate`, `none`) | `update` |

### 1. Manual Deployment (Ubuntu VM)
1. **Install JRE 17**:
   ```bash
   sudo apt update
   sudo apt install openjdk-17-jre-headless
   ```
2. **Build the JAR**:
   ```bash
   mvn clean package -DskipTests
   ```
3. **Transfer JAR** to VM and run:
   ```bash
   export JWT_SECRET="your_strong_secret"
   export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/issuetracker"
   # ... other env vars
   java -jar target/issuetracker-api-*.jar
   ```
   *(Recommended: Use a systemd service for background execution)*

### 2. Docker Deployment
```bash
# Build image
docker build -t issuetracker-api .

# Run container
docker run -d -p 5002:5002 \
  -e JWT_SECRET="your_strong_secret" \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://db_host:5432/issuetracker" \
  -v /path/to/uploads:/app/uploads \
  --name issuetracker-api issuetracker-api
```

### 3. GitHub Actions (CI/CD)
The repository includes a production workflow in `.github/workflows/deploy.yml`.

**To enable deployment:**
1. **Add GitHub Secrets** in your repository settings (`Settings` > `Secrets and variables` > `Actions`):
   - `VM_HOST`: Public IP or hostname of your Ubuntu VM.
   - `VM_USER`: SSH username (e.g., `ubuntu`).
   - `VM_SSH_KEY`: Private SSH key for access to the VM.
   - `JWT_SECRET`: Strong secret for JWT signing.
   - `DB_URL`: JDBC URL (e.g., `jdbc:postgresql://db_host:5432/issuetracker`).
   - `DB_USER`: Database username.
   - `DB_PASSWORD`: Database password.
   - `CORS_ORIGINS`: Comma-separated list of allowed origins.

2. **Push to `main`**: The workflow will automatically:
   - Build the project using Maven.
   - Build and push a Docker image to the GitHub Container Registry (GHCR).
   - Connect to your VM via SSH, pull the latest image, and restart the container.


### 4. Nginx Configuration (Reverse Proxy)
Assuming Nginx is running on an Ubuntu VM and the Spring app is on port 5002:

Create `/etc/nginx/sites-available/issuetracker`:
```nginx
server {
    listen 80;
    server_name api.yourdomain.com;

    location / {
        proxy_pass http://localhost:5002;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Increase max body size for attachments
        client_max_body_size 12M;
    }
}
```
Enable the site:
```bash
sudo ln -s /etc/nginx/sites-available/issuetracker /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
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
config/      Security, CORS, and OpenAPI configurations
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
