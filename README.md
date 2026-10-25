# ResumeFit (Java / Spring Boot)

Upload a resume (PDF, DOCX or TXT), paste a job description, and get a fit score, the skills you have, the skills you are missing, and what to do about them. Every user has an account and a saved history.

This is the Java rebuild of my Python ResumeFit AI. No LLM or paid API is used, so it never fails because of quota or billing.

## What is inside

| Part | What it does |
|---|---|
| `engine/` | Plain Java scoring logic (no Spring). 100+ skills with aliases, regex matching, cosine similarity. Easy to unit test. |
| `parser/` | Reads text from PDF (PDFBox), DOCX (Apache POI) and TXT |
| `security/` + `config/` | Stateless JWT login, BCrypt passwords, CORS |
| `user/` | Register, login, `/api/me` |
| `analysis/` | Upload, history (paged), details, delete, stats. Users can only see their own data |
| `common/` | One JSON error format for every failure |
| `db/migration/` | Flyway SQL, so the schema is versioned |
| `static/index.html` | Small web page that uses the API |

## How the score works

- 75% skill coverage = skills found in both the job description and the resume, divided by skills in the job description
- 25% text similarity = cosine similarity of word counts (0.5 or more counts as full marks)
- If the job description has no known skills, only text similarity is used and the app tells you
- 75+ is STRONG, 50 to 74 is MODERATE, below 50 is WEAK

## Run it

You need Java 21 and Maven.

```bash
mvn spring-boot:run
```

Open http://localhost:8080 (web page) or http://localhost:8080/swagger-ui.html (API docs).

By default it uses an in-memory H2 database, so no MySQL is needed and data is lost on restart. To keep data in a file:

```bash
DB_URL="jdbc:h2:file:./data/resumefit;MODE=MySQL" mvn spring-boot:run
```

With MySQL and Docker (needs a random `JWT_SECRET` of 32+ characters):

```bash
JWT_SECRET="change-me-to-a-long-random-string-123" docker compose up --build
```

## Environment variables

| Name | Meaning | Default |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Database connection | in-memory H2 |
| `JWT_SECRET` | Token signing key, 32+ characters. **Always set this in production** | dev-only value |
| `CORS_ORIGINS` | Allowed browser origins, comma separated | localhost:5173, localhost:3000 |
| `PORT` | Server port | 8080 |

## API

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/auth/register` | no | `email`, `password` (8+), `fullName` |
| POST | `/api/auth/login` | no | returns `token` |
| GET | `/api/me` | yes | |
| POST | `/api/analyses` | yes | multipart: `file`, `jobTitle`, `jobDescription` |
| GET | `/api/analyses?page=0&size=10` | yes | newest first |
| GET | `/api/analyses/{id}` | yes | |
| DELETE | `/api/analyses/{id}` | yes | |
| GET | `/api/analyses/stats` | yes | count, average score, top missing skills |

Send the token as `Authorization: Bearer <token>`.

## Tests

```bash
mvn test
```

- `MatchEngineTest`: skill matching edge cases (Java vs JavaScript, MySQL vs SQL, C++ and C#), scoring, empty input
- `AnalysisFlowTest`: register, login, upload, list, delete, stats, 401 without token, one user cannot read another user's data, bad file type

## Known limits

- Scanned (image-only) PDFs have no text, so they are rejected
- The skill list is a fixed file, `src/main/resources/skills.txt`. If a skill is not in it, it is not counted. Add lines in the format `Category|Name|alias1,alias2`
- No rate limiting on login yet
