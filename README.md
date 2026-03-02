# Course Content Upload System — Backend

Spring Boot REST API for the Silverline IT Course Content Upload System.

---

## Tech Stack

- **Java 17**
- **Spring Boot 3.2**
- **Spring Security + JWT**
- **Spring Data JPA**
- **PostgreSQL**
- **Lombok**

---

## Setup & Run

### 1. Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL running on `localhost:5432`

### 2. Create the Database
```sql
CREATE DATABASE course_content;
```

### 3. Configure application.properties
Update `src/main/resources/application.properties` if needed:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/course_content
spring.datasource.username=postgres
spring.datasource.password=10158
```

### 4. Run the Application
```bash
mvn spring-boot:run
```

The server starts at `http://localhost:8080`

### 5. Run Tests
```bash
mvn test
```

---

## API Endpoints

### Auth (Public)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new instructor |
| POST | `/api/auth/login` | Login and receive JWT token |

### Files (Protected — requires `Authorization: Bearer <token>`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/files/upload` | Upload a file (PDF, MP4, JPG, PNG) |
| GET | `/api/files` | Get all uploaded file metadata |
| GET | `/api/files/{id}` | Get metadata by ID |
| GET | `/api/files/filter?type=pdf` | Filter files by type |
| GET | `/api/files/download/{fileName}` | Download/view a file |
| DELETE | `/api/files/{id}` | Delete a file |

---

## Example Requests

### Register
```json
POST /api/auth/register
{
  "username": "instructor1",
  "password": "password123",
  "email": "instructor@example.com"
}
```

### Login
```json
POST /api/auth/login
{
  "username": "instructor1",
  "password": "password123"
}
```
Response includes `token`. Use it as: `Authorization: Bearer <token>`

### Upload File
```
POST /api/files/upload
Content-Type: multipart/form-data

file: <your file>
description: "Week 1 Lecture"
```

---

## File Constraints

| Property | Value |
|----------|-------|
| Accepted types | PDF, MP4, JPG/JPEG, PNG |
| Max file size | 100 MB |
| Storage | Local disk (`/uploads` directory) |

---

## Database Schema

```sql
CREATE TABLE course_content (
    id SERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    file_url TEXT NOT NULL,
    description VARCHAR(500)
);
```

> Schema is auto-created by Hibernate (`ddl-auto=update`).

---

## Bonus Features

### AWS S3 Storage
To switch from local disk to S3, update `application.properties`:
```properties
app.storage.strategy=s3
aws.s3.bucket-name=my-course-bucket
aws.s3.region=us-east-1
aws.s3.access-key=AKIAIOSFODNN7EXAMPLE
aws.s3.secret-key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```
The storage strategy is pluggable — `local` and `s3` share the same `StorageService` interface with no changes to the controllers or business logic.

---

## Project Structure

```
src/
├── main/java/com/silverline/courseupload/
│   ├── config/
│   │   ├── SecurityConfig.java         # Spring Security + CORS
│   │   ├── JwtUtil.java                # Token generation/validation
│   │   ├── JwtAuthenticationFilter.java
│   │   └── StorageConfig.java          # Selects local or S3 at startup
│   ├── controller/
│   │   ├── FileController.java
│   │   └── AuthController.java
│   ├── dto/            # CourseContentDto, AuthDto, ApiResponse
│   ├── exception/      # Custom exceptions + GlobalExceptionHandler
│   ├── model/          # CourseContent, User entities
│   ├── repository/     # JPA repositories
│   └── service/
│       ├── StorageService.java         # Interface (local/S3 abstraction)
│       ├── LocalStorageService.java    # Local disk (default)
│       ├── S3StorageService.java       # AWS S3 (Bonus)
│       ├── CourseContentService.java
│       ├── AuthService.java
│       └── UserDetailsServiceImpl.java
└── test/
    ├── FileControllerTest.java         # 10 controller endpoint tests
    ├── AuthControllerTest.java         # 7 auth endpoint tests
    ├── CourseContentServiceTest.java   # 8 service logic tests
    └── LocalStorageServiceTest.java    # 10 validation & utility tests
```
