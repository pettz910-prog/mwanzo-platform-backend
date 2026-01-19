# 📡 Complete API Reference - Mwanzo Skills Campus

> **Comprehensive documentation of all REST API endpoints**

---

## Table of Contents

1. [API Overview](#api-overview)
2. [Authentication Endpoints](#authentication-endpoints)
3. [Category Endpoints](#category-endpoints)
4. [Course Endpoints](#course-endpoints)
5. [Video Endpoints](#video-endpoints)
6. [Enrollment Endpoints](#enrollment-endpoints)
7. [Payment Endpoints](#payment-endpoints)
8. [Quiz Endpoints](#quiz-endpoints)
9. [User Endpoints](#user-endpoints)
10. [Error Codes](#error-codes)
11. [Rate Limiting](#rate-limiting)

---

## API Overview

### Base URL
```
Development: http://localhost:8080/api/v1
Production:  https://api.mwanzoskills.co.ke/api/v1
```

### Authentication
Most endpoints require JWT authentication. Include the token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

### Request Format
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

### Response Format
```json
{
  "id": "uuid",
  "name": "User Name",
  "email": "user@example.com",
  "role": "STUDENT",
  "createdAt": "2026-01-18T10:00:00Z"
}
```

### Pagination
```http
GET /api/v1/courses?page=0&size=12&sort=createdAt&direction=desc
```

Response includes pagination metadata:
```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 9,
  "size": 12,
  "number": 0,
  "first": true,
  "last": false,
  "empty": false
}
```

### Error Response Format
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "Invalid email format"
    }
  ],
  "timestamp": "2026-01-18T10:00:00Z"
}
```

---

## Authentication Endpoints

### Register User
Creates a new user account.

**Endpoint:** `POST /api/v1/auth/register`

**Authentication:** None (Public)

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePassword123!",
  "phone": "254712345678",
  "role": "STUDENT"
}
```

**Field Validations:**
- `name`: Required, 2-100 characters
- `email`: Required, valid email format
- `password`: Required, min 8 characters
- `phone`: Optional, valid Kenyan phone number
- `role`: Required, one of: STUDENT, INSTRUCTOR, EMPLOYER

**Success Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "John Doe",
  "email": "john@example.com",
  "role": "STUDENT",
  "isEmailVerified": false,
  "profilePictureUrl": null
}
```

**Error Responses:**
- `400 Bad Request` - Validation errors
- `409 Conflict` - Email already exists

**Example cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "SecurePassword123!",
    "phone": "254712345678",
    "role": "STUDENT"
  }'
```

---

### Login
Authenticates a user and returns a JWT token.

**Endpoint:** `POST /api/v1/auth/login`

**Authentication:** None (Public)

**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "SecurePassword123!"
}
```

**Success Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "John Doe",
  "email": "john@example.com",
  "role": "STUDENT",
  "isEmailVerified": true
}
```

**Error Responses:**
- `400 Bad Request` - Missing credentials
- `401 Unauthorized` - Invalid credentials
- `403 Forbidden` - Account locked or disabled

**Account Locking:**
After 5 failed login attempts, the account is locked for 30 minutes.

**Example cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePassword123!"
  }'
```

---

### Get Current User
Returns the authenticated user's profile.

**Endpoint:** `GET /api/v1/auth/me`

**Authentication:** Required (JWT)

**Success Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "254712345678",
  "profilePictureUrl": "https://s3.amazonaws.com/...",
  "role": "STUDENT",
  "isEmailVerified": true,
  "isActive": true,
  "lastLoginAt": "2026-01-18T10:00:00Z",
  "createdAt": "2026-01-01T12:00:00Z"
}
```

**Error Responses:**
- `401 Unauthorized` - Invalid or missing token

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## Category Endpoints

### List All Categories
Returns all active course categories.

**Endpoint:** `GET /api/v1/categories`

**Authentication:** None (Public)

**Query Parameters:**
- None

**Success Response (200 OK):**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Web Development",
    "description": "Learn to build modern websites and web applications",
    "slug": "web-development",
    "courseCount": 25,
    "icon": "💻",
    "createdAt": "2026-01-01T12:00:00Z"
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "name": "Data Science",
    "description": "Master data analysis and machine learning",
    "slug": "data-science",
    "courseCount": 18,
    "icon": "📊",
    "createdAt": "2026-01-01T12:00:00Z"
  }
]
```

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/v1/categories
```

---

### Get Category by ID
Returns detailed information about a specific category.

**Endpoint:** `GET /api/v1/categories/{id}`

**Authentication:** None (Public)

**Path Parameters:**
- `id` (UUID): Category ID

**Success Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Web Development",
  "description": "Learn to build modern websites and web applications",
  "slug": "web-development",
  "courseCount": 25,
  "icon": "💻",
  "createdAt": "2026-01-01T12:00:00Z"
}
```

**Error Responses:**
- `404 Not Found` - Category not found

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/v1/categories/550e8400-e29b-41d4-a716-446655440000
```

---

### Create Category (Admin Only)
Creates a new course category.

**Endpoint:** `POST /api/v1/categories`

**Authentication:** Required (Admin Role)

**Request Body:**
```json
{
  "name": "Mobile Development",
  "description": "Build iOS and Android applications",
  "icon": "📱"
}
```

**Success Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440002",
  "name": "Mobile Development",
  "description": "Build iOS and Android applications",
  "slug": "mobile-development",
  "courseCount": 0,
  "icon": "📱",
  "createdAt": "2026-01-18T10:00:00Z"
}
```

**Error Responses:**
- `400 Bad Request` - Validation errors
- `401 Unauthorized` - Not authenticated
- `403 Forbidden` - Not admin
- `409 Conflict` - Category name already exists

---

## Course Endpoints

### List All Published Courses
Returns paginated list of all published courses.

**Endpoint:** `GET /api/v1/courses`

**Authentication:** None (Public)

**Query Parameters:**
- `page` (int, default: 0): Page number (0-indexed)
- `size` (int, default: 12): Items per page
- `sortBy` (string, default: createdAt): Sort field
- `direction` (string, default: desc): Sort direction (asc/desc)

**Success Response (200 OK):**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Complete Web Development Bootcamp",
      "slug": "complete-web-development-bootcamp",
      "shortDescription": "Learn HTML, CSS, JavaScript, React, Node.js and more",
      "thumbnailUrl": "https://cdn.mwanzoskills.co.ke/thumbnails/...",
      "price": 5000.00,
      "originalPrice": 8000.00,
      "discountPercentage": 37,
      "level": "BEGINNER",
      "language": "English",
      "durationMinutes": 1200,
      "lectureCount": 156,
      "enrollmentCount": 523,
      "averageRating": 4.8,
      "ratingCount": 127,
      "isFeatured": true,
      "isFree": false,
      "category": {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "name": "Web Development",
        "slug": "web-development",
        "courseCount": 25
      },
      "instructorId": "550e8400-e29b-41d4-a716-446655440002",
      "instructorName": "Jane Smith"
    }
  ],
  "totalElements": 100,
  "totalPages": 9,
  "size": 12,
  "number": 0,
  "first": true,
  "last": false,
  "empty": false
}
```

**Example cURL:**
```bash
curl -X GET "http://localhost:8080/api/v1/courses?page=0&size=12&sortBy=createdAt&direction=desc"
```

---

### Get Course by ID
Returns detailed information about a specific course.

**Endpoint:** `GET /api/v1/courses/{id}`

**Authentication:** None (Public)

**Path Parameters:**
- `id` (UUID): Course ID

**Success Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Complete Web Development Bootcamp",
  "slug": "complete-web-development-bootcamp",
  "description": "Master modern web development with hands-on projects...",
  "shortDescription": "Learn HTML, CSS, JavaScript, React, Node.js and more",
  "learningObjectives": [
    "Build responsive websites from scratch",
    "Master JavaScript and modern ES6+ features",
    "Create full-stack applications with React and Node.js"
  ],
  "requirements": [
    "Basic computer skills",
    "Access to a computer with internet"
  ],
  "thumbnailUrl": "https://cdn.mwanzoskills.co.ke/thumbnails/...",
  "previewVideoUrl": "https://cdn.mwanzoskills.co.ke/previews/...",
  "price": 5000.00,
  "originalPrice": 8000.00,
  "discountPercentage": 37,
  "isFree": false,
  "level": "BEGINNER",
  "language": "English",
  "status": "PUBLISHED",
  "durationMinutes": 1200,
  "lectureCount": 156,
  "enrollmentCount": 523,
  "averageRating": 4.8,
  "ratingCount": 127,
  "isFeatured": true,
  "isPublished": true,
  "category": {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "name": "Web Development",
    "slug": "web-development",
    "courseCount": 25
  },
  "instructorId": "550e8400-e29b-41d4-a716-446655440002",
  "instructorName": "Jane Smith",
  "instructorBio": "Full-stack developer with 10 years of experience",
  "publishedAt": "2026-01-01T12:00:00Z",
  "createdAt": "2025-12-15T10:00:00Z",
  "updatedAt": "2026-01-15T14:30:00Z"
}
```

**Error Responses:**
- `404 Not Found` - Course not found

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/v1/courses/550e8400-e29b-41d4-a716-446655440000
```

---

### Get Course by Slug
Returns course details using SEO-friendly slug.

**Endpoint:** `GET /api/v1/courses/slug/{slug}`

**Authentication:** None (Public)

**Path Parameters:**
- `slug` (string): Course slug (e.g., "complete-web-development-bootcamp")

**Success Response:** Same as Get Course by ID

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/v1/courses/slug/complete-web-development-bootcamp
```

---

### Search Courses
Search courses by keyword in title or description.

**Endpoint:** `GET /api/v1/courses/search`

**Authentication:** None (Public)

**Query Parameters:**
- `q` (string, required): Search keyword
- `page` (int, default: 0): Page number
- `size` (int, default: 12): Items per page
- `sortBy` (string, default: createdAt): Sort field
- `direction` (string, default: desc): Sort direction

**Success Response (200 OK):** Paginated course list (same format as List All)

**Example cURL:**
```bash
curl -X GET "http://localhost:8080/api/v1/courses/search?q=javascript&page=0&size=12"
```

---

### Get Courses by Category
Returns courses in a specific category.

**Endpoint:** `GET /api/v1/courses/category/{categoryId}`

**Authentication:** None (Public)

**Path Parameters:**
- `categoryId` (UUID): Category ID

**Query Parameters:** Same as List All Courses

**Success Response:** Paginated course list

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/v1/courses/category/550e8400-e29b-41d4-a716-446655440001
```

---

### Get Featured Courses
Returns courses marked as featured by admins.

**Endpoint:** `GET /api/v1/courses/featured`

**Authentication:** None (Public)

**Query Parameters:**
- `page` (int, default: 0)
- `size` (int, default: 8)

**Success Response:** Paginated course list

**Example cURL:**
```bash
curl -X GET "http://localhost:8080/api/v1/courses/featured?size=8"
```

---

### Get Popular Courses
Returns courses sorted by enrollment count.

**Endpoint:** `GET /api/v1/courses/popular`

**Authentication:** None (Public)

**Query Parameters:**
- `page` (int, default: 0)
- `size` (int, default: 12)

**Success Response:** Paginated course list

**Example cURL:**
```bash
curl -X GET "http://localhost:8080/api/v1/courses/popular?size=12"
```

---

### Get Free Courses
Returns courses with price = 0.

**Endpoint:** `GET /api/v1/courses/free`

**Authentication:** None (Public)

**Query Parameters:**
- `page` (int, default: 0)
- `size` (int, default: 12)

**Success Response:** Paginated course list

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/v1/courses/free
```

---

### Create Course (Instructor Only)
Creates a new course.

**Endpoint:** `POST /api/v1/courses/instructor/{instructorId}`

**Authentication:** Required (Instructor Role)

**Path Parameters:**
- `instructorId` (UUID): Instructor's user ID

**Request Body:**
```json
{
  "title": "Advanced React Patterns",
  "description": "Master advanced React patterns and best practices",
  "shortDescription": "Learn hooks, context, HOCs, render props and more",
  "categoryId": "550e8400-e29b-41d4-a716-446655440001",
  "price": 3500.00,
  "originalPrice": 5000.00,
  "level": "INTERMEDIATE",
  "language": "English",
  "learningObjectives": [
    "Master React hooks",
    "Understand component composition",
    "Build scalable applications"
  ],
  "requirements": [
    "Basic React knowledge",
    "JavaScript ES6+"
  ]
}
```

**Success Response (201 Created):** Course detail DTO

**Error Responses:**
- `400 Bad Request` - Validation errors
- `401 Unauthorized` - Not authenticated
- `403 Forbidden` - Not instructor
- `404 Not Found` - Category not found

**Example cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/courses/instructor/550e8400-e29b-41d4-a716-446655440002 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Advanced React Patterns",
    "description": "Master advanced React patterns...",
    "categoryId": "550e8400-e29b-41d4-a716-446655440001",
    "price": 3500.00,
    "level": "INTERMEDIATE"
  }'
```

---

## Video Endpoints

### Get Course Sections
Returns all sections (modules) for a course.

**Endpoint:** `GET /api/v1/videos/courses/{courseId}/sections`

**Authentication:** Required (Enrolled students only)

**Path Parameters:**
- `courseId` (UUID): Course ID

**Success Response (200 OK):**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Introduction to Web Development",
    "description": "Get started with the basics",
    "displayOrder": 1,
    "totalDurationMinutes": 120,
    "videoCount": 8,
    "videos": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "sectionId": "550e8400-e29b-41d4-a716-446655440000",
        "title": "Welcome to the Course",
        "description": "Introduction and course overview",
        "displayOrder": 1,
        "durationSeconds": 300,
        "videoUrl": "https://cdn.mwanzoskills.co.ke/videos/...",
        "streamingUrl": "https://cdn.mwanzoskills.co.ke/hls/.../playlist.m3u8",
        "thumbnailUrl": "https://cdn.mwanzoskills.co.ke/thumbnails/...",
        "isPreview": true,
        "isCompleted": false,
        "progressPercentage": 0,
        "lastPositionSeconds": 0,
        "processingStatus": "READY"
      }
    ]
  }
]
```

**Error Responses:**
- `401 Unauthorized` - Not authenticated
- `403 Forbidden` - Not enrolled in course
- `404 Not Found` - Course not found

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/v1/videos/courses/550e8400-e29b-41d4-a716-446655440000/sections \
  -H "Authorization: Bearer <token>"
```

---

### Get Preview Videos
Returns free preview videos for a course (no enrollment required).

**Endpoint:** `GET /api/v1/videos/courses/{courseId}/preview`

**Authentication:** None (Public)

**Path Parameters:**
- `courseId` (UUID): Course ID

**Success Response (200 OK):**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "sectionId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Course Introduction",
    "description": "See what you'll learn",
    "displayOrder": 1,
    "durationSeconds": 300,
    "videoUrl": "https://cdn.mwanzoskills.co.ke/videos/...",
    "streamingUrl": "https://cdn.mwanzoskills.co.ke/hls/.../playlist.m3u8",
    "thumbnailUrl": "https://cdn.mwanzoskills.co.ke/thumbnails/...",
    "isPreview": true,
    "processingStatus": "READY"
  }
]
```

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/v1/videos/courses/550e8400-e29b-41d4-a716-446655440000/preview
```

---

### Get Section Videos
Returns all videos in a specific section.

**Endpoint:** `GET /api/v1/videos/sections/{sectionId}/videos`

**Authentication:** Required (Enrolled students only)

**Path Parameters:**
- `sectionId` (UUID): Section ID

**Success Response:** Array of video DTOs (same format as in Get Course Sections)

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/v1/videos/sections/550e8400-e29b-41d4-a716-446655440000/videos \
  -H "Authorization: Bearer <token>"
```

---

### Get Video Details
Returns detailed information about a specific video.

**Endpoint:** `GET /api/v1/videos/{videoId}`

**Authentication:** Required (Enrolled students or public if preview)

**Path Parameters:**
- `videoId` (UUID): Video ID

**Success Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "sectionId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Welcome to the Course",
  "description": "Introduction and course overview",
  "displayOrder": 1,
  "durationSeconds": 300,
  "videoUrl": "https://cdn.mwanzoskills.co.ke/videos/...",
  "streamingUrl": "https://cdn.mwanzoskills.co.ke/hls/.../playlist.m3u8",
  "thumbnailUrl": "https://cdn.mwanzoskills.co.ke/thumbnails/...",
  "isPreview": true,
  "isCompleted": false,
  "progressPercentage": 45,
  "lastPositionSeconds": 135,
  "processingStatus": "READY"
}
```

**Error Responses:**
- `401 Unauthorized` - Not authenticated
- `403 Forbidden` - No access to video
- `404 Not Found` - Video not found

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/v1/videos/550e8400-e29b-41d4-a716-446655440001 \
  -H "Authorization: Bearer <token>"
```

---

### Update Video Progress
Saves student's watch progress for a video.

**Endpoint:** `POST /api/v1/videos/{videoId}/progress`

**Authentication:** Required

**Path Parameters:**
- `videoId` (UUID): Video ID

**Request Body:**
```json
{
  "studentId": "550e8400-e29b-41d4-a716-446655440010",
  "positionSeconds": 135
}
```

**Success Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440020",
  "studentId": "550e8400-e29b-41d4-a716-446655440010",
  "videoId": "550e8400-e29b-41d4-a716-446655440001",
  "positionSeconds": 135,
  "completed": false,
  "lastWatchedAt": "2026-01-18T10:30:00Z"
}
```

**Auto-Completion:**
- If `positionSeconds` ≥ 80% of video duration, marks video as completed
- Updates enrollment progress
- Checks if all videos completed → marks course as complete

**Example cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/videos/550e8400-e29b-41d4-a716-446655440001/progress \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "550e8400-e29b-41d4-a716-446655440010",
    "positionSeconds": 135
  }'
```

---

### Batch Update Progress
Updates progress for multiple videos in one request.

**Endpoint:** `POST /api/v1/videos/progress/batch`

**Authentication:** Required

**Request Body:**
```json
{
  "studentId": "550e8400-e29b-41d4-a716-446655440010",
  "updates": [
    {
      "videoId": "550e8400-e29b-41d4-a716-446655440001",
      "positionSeconds": 135
    },
    {
      "videoId": "550e8400-e29b-41d4-a716-446655440002",
      "positionSeconds": 45
    }
  ]
}
```

**Success Response (200 OK):**
```json
{
  "successCount": 2,
  "failedCount": 0,
  "totalCount": 2
}
```

**Use Cases:**
- When user closes browser tab
- Periodic backup save
- Page unload event

**Example cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/videos/progress/batch \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "550e8400-e29b-41d4-a716-446655440010",
    "updates": [
      {"videoId": "550e8400-e29b-41d4-a716-446655440001", "positionSeconds": 135},
      {"videoId": "550e8400-e29b-41d4-a716-446655440002", "positionSeconds": 45}
    ]
  }'
```

---

### Get Presigned Upload URL (Instructor)
Generates a presigned S3 URL for video upload.

**Endpoint:** `POST /api/v1/videos/upload-url/video`

**Authentication:** Required (Instructor Role)

**Request Body:**
```json
{
  "fileName": "lesson-01-introduction.mp4",
  "contentType": "video/mp4"
}
```

**Success Response (200 OK):**
```json
{
  "presignedUrl": "https://s3.amazonaws.com/mwanzo-videos/...",
  "s3Key": "videos/550e8400-e29b-41d4-a716-446655440000/lesson-01-introduction.mp4",
  "bucket": "mwanzo-videos",
  "expiresAt": "2026-01-18T11:00:00Z"
}
```

**URL Expiration:** 15 minutes

**Example cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/videos/upload-url/video \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "fileName": "lesson-01-introduction.mp4",
    "contentType": "video/mp4"
  }'
```

**Upload Process:**
1. Get presigned URL from this endpoint
2. Upload video file directly to S3 using presigned URL
3. Create video record with the S3 key

---

### Get Presigned Upload URL for Thumbnail
Same as video upload, but for thumbnails.

**Endpoint:** `POST /api/v1/videos/upload-url/thumbnail`

**Request Body:**
```json
{
  "fileName": "lesson-01-thumbnail.jpg",
  "contentType": "image/jpeg"
}
```

**Response:** Same format as video upload URL

---

## Enrollment Endpoints

### Enroll in Course
Enrolls student in a course (creates pending enrollment if paid).

**Endpoint:** `POST /api/v1/enrollments/{courseId}`

**Authentication:** Required

**Path Parameters:**
- `courseId` (UUID): Course ID

**Request Body:**
```json
{
  "courseId": "550e8400-e29b-41d4-a716-446655440000",
  "paymentMethod": "MPESA",
  "phoneNumber": "254712345678"
}
```

**For Free Courses:**
```json
{
  "courseId": "550e8400-e29b-41d4-a716-446655440000",
  "paymentMethod": "FREE"
}
```

**Success Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440030",
  "studentId": "550e8400-e29b-41d4-a716-446655440010",
  "courseId": "550e8400-e29b-41d4-a716-446655440000",
  "courseTitle": "Complete Web Development Bootcamp",
  "courseSlug": "complete-web-development-bootcamp",
  "courseThumbnailUrl": "https://cdn.mwanzoskills.co.ke/thumbnails/...",
  "instructorName": "Jane Smith",
  "pricePaid": 5000.00,
  "status": "ACTIVE",
  "progressPercentage": 0,
  "videosCompleted": false,
  "quizzesCompleted": false,
  "isCompleted": false,
  "createdAt": "2026-01-18T10:00:00Z"
}
```

**For Paid Courses:**
- Status will be `PENDING_PAYMENT`
- Payment record will be created
- M-Pesa STK Push will be triggered
- Enrollment activated after successful payment

**Error Responses:**
- `400 Bad Request` - Already enrolled
- `401 Unauthorized` - Not authenticated
- `404 Not Found` - Course not found

**Example cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/enrollments/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": "550e8400-e29b-41d4-a716-446655440000",
    "paymentMethod": "MPESA",
    "phoneNumber": "254712345678"
  }'
```

---

### Get My Enrollments
Returns all enrollments for the authenticated user.

**Endpoint:** `GET /api/v1/enrollments`

**Authentication:** Required

**Query Parameters:**
- `page` (int, default: 0)
- `size` (int, default: 12)

**Success Response (200 OK):** Paginated enrollment list

**Example cURL:**
```bash
curl -X GET "http://localhost:8080/api/v1/enrollments?page=0&size=12" \
  -H "Authorization: Bearer <token>"
```

---

### Get Enrollment by ID
Returns detailed information about a specific enrollment.

**Endpoint:** `GET /api/v1/enrollments/{id}`

**Authentication:** Required (Own enrollment only)

**Path Parameters:**
- `id` (UUID): Enrollment ID

**Success Response:** Enrollment DTO (same format as enroll response)

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/v1/enrollments/550e8400-e29b-41d4-a716-446655440030 \
  -H "Authorization: Bearer <token>"
```

---

### Check Enrollment Status
Checks if student is enrolled in a course.

**Endpoint:** `GET /api/v1/enrollments/check/{courseId}`

**Authentication:** Required

**Path Parameters:**
- `courseId` (UUID): Course ID

**Success Response (200 OK):**
```json
{
  "isEnrolled": true,
  "hasAccess": true
}
```

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/v1/enrollments/check/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <token>"
```

---

## Payment Endpoints

### Initiate Payment
Initiates M-Pesa STK Push payment.

**Endpoint:** `POST /api/v1/payments`

**Authentication:** Required

**Request Body:**
```json
{
  "enrollmentId": "550e8400-e29b-41d4-a716-446655440030",
  "phoneNumber": "254712345678"
}
```

**Success Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440040",
  "enrollmentId": "550e8400-e29b-41d4-a716-446655440030",
  "transactionReference": "TXN-1705564800-ABC123",
  "checkoutRequestId": "ws_CO_18012026103000000712345678",
  "amount": 5000.00,
  "currency": "KES",
  "phoneNumber": "254712345678",
  "paymentStatus": "PENDING",
  "createdAt": "2026-01-18T10:30:00Z",
  "updatedAt": "2026-01-18T10:30:00Z"
}
```

**Payment Flow:**
1. Backend calls PayHero API
2. PayHero sends STK Push to user's phone
3. User enters M-Pesa PIN on phone
4. Payment processed
5. PayHero sends webhook to backend
6. Backend updates payment status
7. Frontend polls payment status

**Error Responses:**
- `400 Bad Request` - Invalid request
- `401 Unauthorized` - Not authenticated
- `404 Not Found` - Enrollment not found
- `409 Conflict` - Payment already completed

**Example cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "enrollmentId": "550e8400-e29b-41d4-a716-446655440030",
    "phoneNumber": "254712345678"
  }'
```

---

### Check Payment Status
Polls payment status (used by frontend).

**Endpoint:** `GET /api/v1/payments/status/{transactionReference}`

**Authentication:** Required

**Path Parameters:**
- `transactionReference` (string): Transaction reference from initiate payment

**Success Response (200 OK):**
```json
{
  "paymentStatus": "SUCCESS",
  "transactionReference": "TXN-1705564800-ABC123",
  "mpesaReceiptNumber": "QGD3K8WXYZ",
  "shouldContinuePolling": false
}
```

**Payment Statuses:**
- `PENDING` - Payment in progress
- `SUCCESS` - Payment completed
- `FAILED` - Payment failed

**Polling Recommendations:**
- Poll every 3 seconds
- Stop polling when `shouldContinuePolling` is false
- Timeout after 5 minutes

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/v1/payments/status/TXN-1705564800-ABC123 \
  -H "Authorization: Bearer <token>"
```

---

### Payment Webhook (Internal)
Receives payment callbacks from PayHero.

**Endpoint:** `POST /api/v1/payments/callback`

**Authentication:** None (PayHero signature validation)

**This endpoint is called by PayHero, not the frontend.**

---

## Quiz Endpoints

### Get Course Quizzes
Returns all quizzes for a course.

**Endpoint:** `GET /api/v1/quizzes/courses/{courseId}`

**Authentication:** Required (Enrolled students only)

**Path Parameters:**
- `courseId` (UUID): Course ID

**Success Response (200 OK):**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440050",
    "courseId": "550e8400-e29b-41d4-a716-446655440000",
    "sectionId": "550e8400-e29b-41d4-a716-446655440001",
    "title": "JavaScript Fundamentals Quiz",
    "description": "Test your knowledge of JavaScript basics",
    "timeLimitMinutes": 30,
    "passingScore": 70,
    "maxAttempts": 3,
    "displayOrder": 1,
    "questionCount": 10,
    "totalPoints": 100,
    "isPublished": true,
    "shuffleQuestions": true,
    "shuffleAnswers": true,
    "showCorrectAnswers": true
  }
]
```

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/v1/quizzes/courses/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <token>"
```

---

### Get Quiz Details
Returns detailed information about a quiz (without answers).

**Endpoint:** `GET /api/v1/quizzes/{quizId}`

**Authentication:** Required (Enrolled students only)

**Path Parameters:**
- `quizId` (UUID): Quiz ID

**Success Response:** Quiz DTO with questions (but not correct answers until after submission)

---

### Start Quiz Attempt
Creates a new quiz attempt for a student.

**Endpoint:** `POST /api/v1/quizzes/{quizId}/start`

**Authentication:** Required

**Path Parameters:**
- `quizId` (UUID): Quiz ID

**Request Body:**
```json
{
  "studentId": "550e8400-e29b-41d4-a716-446655440010"
}
```

**Success Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440060",
  "studentId": "550e8400-e29b-41d4-a716-446655440010",
  "quizId": "550e8400-e29b-41d4-a716-446655440050",
  "startedAt": "2026-01-18T11:00:00Z",
  "attemptNumber": 1,
  "questions": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440070",
      "quizId": "550e8400-e29b-41d4-a716-446655440050",
      "questionText": "What is the output of: console.log(typeof null)?",
      "displayOrder": 1,
      "points": 10,
      "answers": [
        {
          "id": "550e8400-e29b-41d4-a716-446655440080",
          "answerText": "null",
          "displayOrder": 1
        },
        {
          "id": "550e8400-e29b-41d4-a716-446655440081",
          "answerText": "object",
          "displayOrder": 2
        },
        {
          "id": "550e8400-e29b-41d4-a716-446655440082",
          "answerText": "undefined",
          "displayOrder": 3
        }
      ]
    }
  ]
}
```

**Note:** Correct answers are NOT included in the response.

**Example cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/quizzes/550e8400-e29b-41d4-a716-446655440050/start \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "550e8400-e29b-41d4-a716-446655440010"
  }'
```

---

### Answer Question
Submits answer for a single question.

**Endpoint:** `POST /api/v1/quizzes/questions/{questionId}/answer`

**Authentication:** Required

**Path Parameters:**
- `questionId` (UUID): Question ID

**Request Body:**
```json
{
  "attemptId": "550e8400-e29b-41d4-a716-446655440060",
  "selectedAnswerId": "550e8400-e29b-41d4-a716-446655440081"
}
```

**Success Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440090",
  "attemptId": "550e8400-e29b-41d4-a716-446655440060",
  "questionId": "550e8400-e29b-41d4-a716-446655440070",
  "selectedAnswerId": "550e8400-e29b-41d4-a716-446655440081",
  "isCorrect": true,
  "points": 10,
  "answeredAt": "2026-01-18T11:05:00Z"
}
```

**Example cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/quizzes/questions/550e8400-e29b-41d4-a716-446655440070/answer \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "attemptId": "550e8400-e29b-41d4-a716-446655440060",
    "selectedAnswerId": "550e8400-e29b-41d4-a716-446655440081"
  }'
```

---

### Submit Quiz
Finishes quiz attempt and calculates final score.

**Endpoint:** `POST /api/v1/quizzes/attempts/{attemptId}/submit`

**Authentication:** Required

**Path Parameters:**
- `attemptId` (UUID): Attempt ID

**Success Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440060",
  "studentId": "550e8400-e29b-41d4-a716-446655440010",
  "quizId": "550e8400-e29b-41d4-a716-446655440050",
  "startedAt": "2026-01-18T11:00:00Z",
  "submittedAt": "2026-01-18T11:15:00Z",
  "score": 85,
  "passed": true,
  "attemptNumber": 1,
  "correctAnswers": 9,
  "totalQuestions": 10,
  "questionResults": [
    {
      "questionId": "550e8400-e29b-41d4-a716-446655440070",
      "questionText": "What is the output of: console.log(typeof null)?",
      "selectedAnswer": "object",
      "correctAnswer": "object",
      "isCorrect": true,
      "points": 10
    }
  ]
}
```

**Example cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/quizzes/attempts/550e8400-e29b-41d4-a716-446655440060/submit \
  -H "Authorization: Bearer <token>"
```

---

### Get Student Attempts
Returns all attempts for a quiz by a student.

**Endpoint:** `GET /api/v1/quizzes/{quizId}/attempts`

**Authentication:** Required

**Path Parameters:**
- `quizId` (UUID): Quiz ID

**Success Response:** Array of quiz attempt DTOs

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/v1/quizzes/550e8400-e29b-41d4-a716-446655440050/attempts \
  -H "Authorization: Bearer <token>"
```

---

### Check Quiz Eligibility
Checks if student can start a new quiz attempt.

**Endpoint:** `GET /api/v1/quizzes/{quizId}/can-start`

**Authentication:** Required

**Path Parameters:**
- `quizId` (UUID): Quiz ID

**Success Response (200 OK):**
```json
{
  "canStart": true,
  "attemptCount": 1,
  "maxAttempts": 3,
  "reason": null
}
```

**If Cannot Start:**
```json
{
  "canStart": false,
  "attemptCount": 3,
  "maxAttempts": 3,
  "reason": "Maximum attempts reached"
}
```

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/v1/quizzes/550e8400-e29b-41d4-a716-446655440050/can-start \
  -H "Authorization: Bearer <token>"
```

---

## Error Codes

### HTTP Status Codes

| Code | Meaning | Description |
|------|---------|-------------|
| 200 | OK | Request successful |
| 201 | Created | Resource created successfully |
| 204 | No Content | Request successful, no content to return |
| 400 | Bad Request | Invalid request data or validation error |
| 401 | Unauthorized | Authentication required or invalid token |
| 403 | Forbidden | Authenticated but not authorized |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Resource already exists or state conflict |
| 422 | Unprocessable Entity | Validation error |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error (contact support) |
| 503 | Service Unavailable | Server temporarily unavailable |

### Common Error Responses

#### Validation Error (400)
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "Invalid email format"
    },
    {
      "field": "password",
      "message": "Password must be at least 8 characters"
    }
  ],
  "timestamp": "2026-01-18T10:00:00Z"
}
```

#### Authentication Error (401)
```json
{
  "status": 401,
  "message": "Invalid or expired token",
  "timestamp": "2026-01-18T10:00:00Z"
}
```

#### Authorization Error (403)
```json
{
  "status": 403,
  "message": "You do not have permission to access this resource",
  "timestamp": "2026-01-18T10:00:00Z"
}
```

#### Not Found Error (404)
```json
{
  "status": 404,
  "message": "Course not found with ID: 550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-01-18T10:00:00Z"
}
```

#### Conflict Error (409)
```json
{
  "status": 409,
  "message": "User already enrolled in this course",
  "timestamp": "2026-01-18T10:00:00Z"
}
```

---

## Rate Limiting

### Current Limits (Future Implementation)

| Endpoint Category | Limit | Window |
|-------------------|-------|--------|
| Authentication | 10 requests | 1 minute |
| General API | 100 requests | 1 minute |
| Video Progress | 60 requests | 1 minute |
| Payment | 5 requests | 1 minute |

### Rate Limit Headers (Future)
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1705564800
```

### Rate Limit Exceeded Response
```json
{
  "status": 429,
  "message": "Rate limit exceeded. Please try again in 60 seconds.",
  "retryAfter": 60,
  "timestamp": "2026-01-18T10:00:00Z"
}
```

---

## API Versioning

**Current Version:** `v1`

All endpoints are prefixed with `/api/v1/`

**Future versions will be available at:**
- `/api/v2/` - Breaking changes
- `/api/v1/` - Maintained for backward compatibility

---

## Additional Resources

### Postman Collection
Download the complete Postman collection:
- **[Mwanzo API Collection](./postman/Mwanzo-API.postman_collection.json)**

### SDK/Client Libraries
- **TypeScript/JavaScript:** See `src/mwanzo-platform-main/src/services/api/`
- **Java:** Use RestTemplate or WebClient
- **Python:** Coming soon
- **PHP:** Coming soon

### Webhooks
- **[Payment Webhooks](./integration/PAYHERO.md)** - PayHero callback handling
- **[Video Processing Webhooks](./integration/MEDIACONVERT.md)** - AWS MediaConvert notifications

### Testing
- **[API Testing Guide](../testing/API_TESTING.md)** - Comprehensive testing strategies
- **Test Accounts:** See [Test Accounts](../testing/TEST_ACCOUNTS.md)

---

## Support

### Getting Help
- **Technical Documentation:** [Full Documentation Index](../../DOCUMENTATION_INDEX.md)
- **Integration Issues:** [Integration Guide](../integration/API_INTEGRATION.md)
- **Bug Reports:** GitHub Issues
- **Email:** dev@mwanzoskills.co.ke

### Changelog
- **v1.0.0** (2026-01-18) - Initial API release
  - All core endpoints implemented
  - Authentication, Courses, Videos, Payments, Quizzes
  - Production-ready

---

**API Documentation Version:** 1.0.0
**Last Updated:** January 18, 2026
**Author:** Mwanzo Development Team

📡 **Happy Coding!** 🚀
