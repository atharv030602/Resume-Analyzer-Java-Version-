# ResumeFit (Java / Spring Boot)

Upload a resume, paste a job description, and get a fit score, the skills you already have, the skills you are missing, and what to do about them. Every user has an account and a saved history.

**Live demo:** https://resumefit-v875.onrender.com
**API docs (Swagger):** https://resumefit-v875.onrender.com/swagger-ui.html

> The demo runs on free hosting. The first request can take about a minute to wake up, and data resets whenever the app restarts (it uses an in-memory database there).

This is the Java rebuild of my earlier Python version of ResumeFit AI. It uses no LLM and no paid API, so it cannot fail because of quota, billing or network limits.

<!--
Add screenshots after you save them in a docs/ folder, then remove this comment:
![Result page](docs/result.png)
![Swagger](docs/swagger.png)
-->

## Features

- Register and log in with JWT (stateless, BCrypt password hashing)
- Upload a resume as PDF, DOCX or TXT (max 5 MB)
- Fit score from 0 to 100 with a verdict: STRONG, MODERATE or WEAK
- Matched skills, missing skills, extra skills, and a tip for each missing skill
- Saved history with paging, detail view and delete
- Personal stats: number of analyses, average score, most common missing skills
- Each user can only see their own data
- Clear JSON error messages for every failure
- Small web page and Swagger UI included

## Tech stack

| Area | Tools |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 (Web, Data JPA, Security, Validation, Actuator) |
| Auth | Spring Security, JWT (jjwt), BCrypt |
| Database | MySQL 8 (Docker) or H2 (default), schema versioned with Flyway |
| File parsing | Apache PDFBox (PDF), Apache POI (DOCX) |
| API docs | springdoc OpenAPI (Swagger UI) |
| Testing | JUnit 5, AssertJ, Spring MockMvc |
| DevOps | Docker, Docker Compose, GitHub Actions, Render |

## How it works

```
Browser / API client
        |
        v
  JwtAuthFilter  -->  AuthController / AnalysisController
                                |
                                v
                         AnalysisService
                        /       |        \
           DocumentTextExtractor  MatchEngine   AnalysisRepository --> MySQL / H2
             (PDF, DOCX, TXT)    (pure Java)
```

| Package | What it does |
|---|---|
| `engine/` | The scoring logic in plain Java (no Spring), so it is easy to unit test |
| `parser/` | Turns an uploaded file into text |
| `security/`, `config/` | JWT filter, security rules, CORS, bean setup |
| `user/` | Register, login, `/api/me` |
| `analysis/` | Upload, history, details, delete, stats |
| `common/` | Shared exceptions and one error format |
| `db/migration/` | Flyway SQL files |

## How the score works

- **75% skill coverage:** skills found in both the job description and the resume, divided by all skills in the job description
- **25% text similarity:** cosine similarity of word counts (a value of 0.5 or more counts as full marks)
- If the job description has no known skills, only text similarity is used, and the app tells you
- 75 or more is STRONG, 50 to 74 is MODERATE, below 50 is WEAK

Skill matching uses a dictionary of 100+ skills with aliases (`src/main/resources/skills.txt`). It handles tricky cases, for example:

- "Java" is not matched inside "JavaScript"
- "SQL" is not matched inside "MySQL"
- "C++", "C#" and "Node.js" are matched correctly
- "Spring Boot" is matched even when a PDF splits it across two lines

## Design choices

- **Local matching instead of an LLM:** my first version depended on LLM providers, and three of them failed (quota, billing, network). This version is deterministic, free and fast.
- **Scoring engine has no Spring dependency:** it can be tested without starting the application.
- **Flyway owns the schema:** Hibernate does not change tables (`ddl-auto: none`), so database changes are versioned and repeatable.
- **Ownership check on every query:** analyses are always looked up by id and user id, so one user gets a 404 for another user's data.
- **Same login error for wrong email and wrong password:** it does not reveal which emails exist.

## Run it locally

You need Java 21 and Maven.

```bash
git clone https://github.com/atharv030602/Resume-Analyzer-Java-Version-.git
cd Resume-Analyzer-Java-Version-
mvn spring-boot:run
```

Open http://localhost:8080 for the web page, or http://localhost:8080/swagger-ui.html for the API docs.

By default it uses an in-memory H2 database, so you do not need MySQL. Data is lost on restart. To keep data in a file:

```bash
DB_URL="jdbc:h2:file:./data/resumefit;MODE=MySQL" mvn spring-boot:run
```

### With MySQL and Docker

```bash
JWT_SECRET="replace-with-a-random-string-of-32-or-more-characters" docker compose up --build
```

On Windows PowerShell:

```powershell
$env:JWT_SECRET="replace-with-a-random-string-of-32-or-more-characters"
docker compose up --build
```

## Configuration

| Variable | Meaning | Default |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Database connection | in-memory H2 |
| `JWT_SECRET` | Token signing key, 32+ characters. **Always set this in production** | dev-only value |
| `CORS_ORIGINS` | Allowed browser origins, comma separated | `localhost:5173`, `localhost:3000` |
| `PORT` | Server port | 8080 |

## API

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/auth/register` | no | `email`, `password` (8+ characters), `fullName` |
| POST | `/api/auth/login` | no | returns a `token` |
| GET | `/api/me` | yes | current user |
| POST | `/api/analyses` | yes | multipart: `file`, `jobTitle`, `jobDescription` |
| GET | `/api/analyses?page=0&size=10` | yes | newest first |
| GET | `/api/analyses/{id}` | yes | full result |
| DELETE | `/api/analyses/{id}` | yes | |
| GET | `/api/analyses/stats` | yes | count, average score, top missing skills |

Send the token as `Authorization: Bearer <token>`.

Example:

```bash
# register and get a token
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"me@example.com","password":"password123","fullName":"Me"}'

# analyze a resume
curl -X POST http://localhost:8080/api/analyses \
  -H "Authorization: Bearer <token>" \
  -F "file=@resume.pdf" \
  -F "jobTitle=Java Developer" \
  -F "jobDescription=We need a Java engineer with Spring Boot, MySQL and Docker."
```

## Tests

```bash
mvn test
```

17 tests, all passing:

- `MatchEngineTest` (10): skill matching edge cases, scoring, empty input, verdict limits
- `AnalysisFlowTest` (7): register, login, wrong password, duplicate email, short password, upload, list, stats, delete, 401 without a token, one user cannot read another user's data, unsupported file type

A GitHub Actions workflow (`.github/workflows/ci.yml`) runs the build, the tests and a Docker build on every push.

## Known limits

- Scanned (image-only) PDFs have no text, so they are rejected
- The skill list is a fixed file. If a skill is not in `skills.txt`, it is not counted. To add one, add a line in the format `Category|Name|alias1,alias2`
- No rate limiting on login yet
- The free demo database resets on restart

## Possible next steps

- Rate limiting on login and upload
- Refresh tokens and logout
- Paged skill-gap report across all analyses
- A React frontend using the same API (CORS is already configured)
