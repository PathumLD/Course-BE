# Course Content Upload System — Backend

A Spring Boot REST API for uploading and managing course content files (PDF, MP4, JPG, PNG) with JWT authentication and pluggable storage (local disk or AWS S3).

---

## Tech Stack

| Layer | Technology                               |
|---|------------------------------------------|
| Language | Java 17                                  |
| Framework | Spring Boot 3.2                          |
| Security | Spring Security + JWT (jjwt 0.12)        |
| Persistence | Spring Data JPA + PostgreSQL             |
| Storage | Local disk (default) / AWS S3 (optional) |
| Utilities | Lombok, spring-dotenv                    |

---

## Project Structure

```
course-upload-backend/
├── .env                        ← your local secrets (NOT committed)
├── .env.example                ← template to copy from (committed)
├── .gitignore
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/courseupload/
    │   │   ├── CourseUploadApplication.java
    │   │   ├── config/
    │   │   │   ├── JwtAuthenticationFilter.java
    │   │   │   ├── JwtUtil.java
    │   │   │   ├── SecurityConfig.java
    │   │   │   └── StorageConfig.java      ← selects local or S3 at startup
    │   │   ├── controller/
    │   │   │   ├── AuthController.java
    │   │   │   └── FileController.java
    │   │   ├── dto/
    │   │   │   ├── ApiResponse.java
    │   │   │   ├── AuthDto.java
    │   │   │   └── CourseContentDto.java
    │   │   ├── exception/
    │   │   │   ├── FileStorageException.java
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   ├── InvalidFileException.java
    │   │   │   └── ResourceNotFoundException.java
    │   │   ├── model/
    │   │   │   ├── CourseContent.java
    │   │   │   └── User.java
    │   │   ├── repository/
    │   │   │   ├── CourseContentRepository.java
    │   │   │   └── UserRepository.java
    │   │   └── service/
    │   │       ├── AuthService.java
    │   │       ├── CourseContentService.java
    │   │       ├── LocalStorageService.java
    │   │       ├── S3StorageService.java
    │   │       ├── StorageService.java     ← shared interface
    │   │       └── UserDetailsServiceImpl.java
    │   └── resources/
    │       └── application.properties
    └── test/java/com/courseupload/
        ├── AuthControllerTest.java
        ├── CourseContentServiceTest.java
        ├── FileControllerTest.java
        └── LocalStorageServiceTest.java
```

---

## Environment Variables

This project uses a `.env` file to keep secrets out of source control. The `application.properties` file is safe to commit — it contains no credentials, only `${VARIABLE}` placeholders that Spring resolves at runtime via the `spring-dotenv` library.

### Setup

**1. Copy the example file:**
```bash
cp .env.example .env
```

**2. Fill in your values in `.env`:**
```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/course_content
DB_USERNAME=postgres
DB_PASSWORD=your_password

# JWT
JWT_SECRET=your_256bit_secret_key
JWT_EXPIRATION=86400000

# Storage: "local" or "s3"
STORAGE_STRATEGY=local

# AWS S3 (only needed if STORAGE_STRATEGY=s3)
AWS_S3_BUCKET_NAME=your-bucket
AWS_S3_REGION=us-east-1
AWS_S3_ACCESS_KEY=your_access_key
AWS_S3_SECRET_KEY=your_secret_key

# Server
SERVER_PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

> `.env` is listed in `.gitignore` and will never be committed. Only `.env.example` (which has no real values) is tracked by Git.

### How it works

`application.properties` contains this line at the top:
```properties
spring.config.import=optional:dotenv:.env
```
This tells Spring Boot to load the `.env` file on startup via `spring-dotenv`. The `optional:` prefix means the app won't crash if `.env` is absent — useful in CI/CD pipelines where variables are injected by the platform directly.

---

## Local Setup & Run

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL running on `localhost:5432`

### Steps

**1. Create the database:**
```sql
CREATE DATABASE course_content;
```

**2. Set up your `.env` file** (see [Environment Variables](#environment-variables) above)

**3. Run the application:**
```bash
mvn spring-boot:run
```

The server starts at `http://localhost:8080`

---

## API Reference

### Authentication (Public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new instructor account |
| POST | `/api/auth/login` | Login and receive a JWT token |

#### Register
```json
POST /api/auth/register
{
  "username": "instructor1",
  "password": "password123",
  "email": "instructor@example.com"
}
```

#### Login
```json
POST /api/auth/login
{
  "username": "instructor1",
  "password": "password123"
}
```
Returns a `token`. Use it on all protected routes:
```
Authorization: Bearer <token>
```

---

### Files (Protected — requires JWT)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/files/upload` | Upload a file |
| GET | `/api/files` | List all uploaded files |
| GET | `/api/files/{id}` | Get file metadata by ID |
| GET | `/api/files/filter?type=pdf` | Filter files by type |
| GET | `/api/files/download/{fileName}` | Download or stream a file |
| DELETE | `/api/files/{id}` | Delete a file |

#### Upload a file
```
POST /api/files/upload
Authorization: Bearer <token>
Content-Type: multipart/form-data

file=<your_file>
description=Week 1 Lecture   (optional)
```

#### Example response
```json
{
  "success": true,
  "message": "File uploaded successfully.",
  "data": {
    "id": 1,
    "originalFileName": "lecture.pdf",
    "fileType": "application/pdf",
    "fileSize": 204800,
    "fileSizeFormatted": "200.0 KB",
    "uploadDate": "2026-03-02T10:00:00",
    "downloadUrl": "http://localhost:8080/api/files/download/uuid-1234.pdf",
    "description": "Week 1 Lecture"
  }
}
```

---

## File Constraints

| Property | Value |
|----------|-------|
| Accepted MIME types | `application/pdf`, `video/mp4`, `image/jpeg`, `image/png` |
| Accepted extensions | `.pdf`, `.mp4`, `.jpg`, `.jpeg`, `.png` |
| Max file size | 100 MB |

---

## Storage Strategies

The app uses a `StorageService` interface with two implementations. Switch between them by setting `STORAGE_STRATEGY` in your `.env`.

### Local disk (default)
```env
STORAGE_STRATEGY=local
```
Files are saved to the `uploads/` directory in the project root.

### AWS S3
```env
STORAGE_STRATEGY=s3
AWS_S3_BUCKET_NAME=my-course-bucket
AWS_S3_REGION=us-east-1
AWS_S3_ACCESS_KEY=AKIAIOSFODNN7EXAMPLE
AWS_S3_SECRET_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```
No code changes are needed — `StorageConfig.java` selects the correct bean at startup based on the strategy value.

---

## Deploying to Production

When deploying to a cloud platform (Render, Railway, Heroku, etc.), do **not** upload your `.env` file. Instead, set each variable in the platform's environment/config settings panel using the same variable names. The `optional:dotenv:.env` import will simply be skipped and Spring will read the system environment variables directly.

---

## Database Schema

Schema is auto-managed by Hibernate (`ddl-auto=update`). For reference:

```sql
CREATE TABLE users (
    id       BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email    VARCHAR(150) NOT NULL UNIQUE,
    role     VARCHAR(20)  NOT NULL
);

CREATE TABLE course_content (
    id                 BIGSERIAL PRIMARY KEY,
    file_name          VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_type          VARCHAR(50)  NOT NULL,
    file_size          BIGINT       NOT NULL,
    upload_date        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    file_url           TEXT         NOT NULL,
    description        VARCHAR(500)
);
```
