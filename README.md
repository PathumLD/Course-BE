# Course Content Upload System — Backend
---

## Prerequisites

Make sure the following are installed on your machine before starting:

| Tool | Version            |
|------|--------------------
| Java JDK | 17 or higher       |
| Apache Maven | 3.8 or higher      |
| PostgreSQL | 14 or higher       |
| Git | Any recent version |

To verify your installations, run:
```bash
java -version
mvn -version
psql --version
git --version
```

---

## Setup Guide

### Step 1 — Clone the Repository

```bash
git clone https://github.com/PathumLD/Course-BE.git
cd Course-BE
```

---

### Step 2 — Create the PostgreSQL Database

Open your PostgreSQL terminal (psql) and run:

```sql
CREATE DATABASE course_content;
```

To open the psql terminal:
```bash
# Windows
psql -U postgres

# macOS / Linux
sudo -u postgres psql
```

> The database tables (`users`, `course_content`) are created **automatically** by Hibernate when the app starts for the first time. You only need to create the empty database.

---

### Step 3 — Set Up Environment Variables

The project uses a `.env` file to store secrets. This file is **not included** in the repository for security reasons. You need to create it yourself.

**3.1 — Copy the example file:**

```bash
# macOS / Linux
cp .env.example .env

# Windows (Command Prompt)
copy .env.example .env

# Windows (PowerShell)
Copy-Item .env.example .env
```

**3.2 — Open `.env` and fill in your values:**

```env
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/course_content
DB_USERNAME=postgres
DB_PASSWORD=your_postgres_password

# JWT Configuration
JWT_SECRET=your_secret_key_min_32_characters_long
JWT_EXPIRATION=86400000

# Storage Strategy — use "local" to save files to disk
STORAGE_STRATEGY=local

# AWS S3 — only required if STORAGE_STRATEGY=s3, otherwise leave as-is
AWS_S3_BUCKET_NAME=your-bucket-name
AWS_S3_REGION=us-east-1
AWS_S3_ACCESS_KEY=your_access_key
AWS_S3_SECRET_KEY=your_secret_key

# Server
SERVER_PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

Replace the following with your actual values:
- `your_postgres_password` → your PostgreSQL password
- `your_secret_key_min_32_characters_long` → any random string of at least 32 characters (used to sign JWT tokens)

> **Never commit your `.env` file.** It is already listed in `.gitignore` so Git will automatically ignore it.

---

### Step 4 — Install Dependencies

Maven will download all required dependencies automatically:

```bash
mvn clean install -DskipTests
```

---

### Step 5 — Run the Application

```bash
mvn spring-boot:run
```

You should see output ending with something like:
```
Started CourseUploadApplication in X.XXX seconds
```

The API is now running at: **`http://localhost:8080`**

---

### Step 6 — Verify It Works

Open a browser or API client (e.g. Postman) and send a request:

```
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123",
  "email": "test@example.com"
}
```

You should receive a `200 OK` response with a JWT token. If so, the setup is complete.

---

## Running the Tests

```bash
mvn test
```

All 40 tests should pass. The test suite covers:
- `AuthControllerTest` — registration and login API
- `FileControllerTest` — file upload, retrieval, download, and deletion API
- `CourseContentServiceTest` — business logic layer
- `LocalStorageServiceTest` — file validation and storage

---

## Project Structure

```
Course-BE/
├── .env                        ← your local secrets (NOT committed)
├── .env.example                ← template — copy this to create .env
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
    │   │   │   └── StorageConfig.java
    │   │   ├── controller/
    │   │   │   ├── AuthController.java
    │   │   │   └── FileController.java
    │   │   ├── dto/
    │   │   ├── exception/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   └── service/
    │   └── resources/
    │       └── application.properties
    └── test/java/com/courseupload/
```

---

## API Reference

### Authentication — Public Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Create a new instructor account |
| POST | `/api/auth/login` | Login and receive a JWT token |

**Register:**
```json
POST /api/auth/register
{
  "username": "instructor1",
  "password": "password123",
  "email": "instructor@example.com"
}
```

**Login:**
```json
POST /api/auth/login
{
  "username": "instructor1",
  "password": "password123"
}
```

Copy the `token` from the login response and include it in all protected requests:
```
Authorization: Bearer <token>
```

---

### Files — Protected Endpoints (JWT required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/files/upload` | Upload a file |
| GET | `/api/files` | List all uploaded files |
| GET | `/api/files/{id}` | Get file metadata by ID |
| GET | `/api/files/filter?type=pdf` | Filter files by type |
| GET | `/api/files/download/{fileName}` | Download a file (public) |
| DELETE | `/api/files/{id}` | Delete a file |

**Upload example (multipart/form-data):**
```
POST /api/files/upload
Authorization: Bearer <token>

file: <your_file>
description: Week 1 Lecture  (optional)
```

**Accepted file types:** `.pdf`, `.mp4`, `.jpg`, `.jpeg`, `.png` — max **100 MB**

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (jjwt 0.12) |
| Persistence | Spring Data JPA + PostgreSQL |
| Storage | Local disk (default) / AWS S3 (optional) |
| Build | Maven |
| Utilities | Lombok, spring-dotenv |

---

## Switching to AWS S3 Storage

By default, uploaded files are saved to an `uploads/` folder on your local disk. To use AWS S3 instead, update your `.env`:

```env
STORAGE_STRATEGY=s3
AWS_S3_BUCKET_NAME=my-bucket-name
AWS_S3_REGION=us-east-1
AWS_S3_ACCESS_KEY=AKIAIOSFODNN7EXAMPLE
AWS_S3_SECRET_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

No code changes needed — the app automatically uses the S3 implementation on startup.

---

## Common Issues

**App fails to start with `NumberFormatException` on `server.port`**
Your `.env` file is missing or not in the project root directory. Make sure it exists at `Course-BE/.env`.

**`password authentication failed for user "postgres"`**
The `DB_PASSWORD` in your `.env` does not match your PostgreSQL password. Update it to match.

**`FATAL: database "course_content" does not exist`**
You haven't created the database yet. Run `CREATE DATABASE course_content;` in psql (see Step 2).

**Port 8080 already in use**
Change `SERVER_PORT=8081` (or any free port) in your `.env` file.