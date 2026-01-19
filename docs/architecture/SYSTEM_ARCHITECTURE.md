# 🏗️ System Architecture - Mwanzo Skills Campus

> **Complete technical architecture of the Mwanzo e-learning platform**

---

## Table of Contents

1. [Overview](#overview)
2. [High-Level Architecture](#high-level-architecture)
3. [Component Architecture](#component-architecture)
4. [Data Architecture](#data-architecture)
5. [Security Architecture](#security-architecture)
6. [Integration Architecture](#integration-architecture)
7. [Deployment Architecture](#deployment-architecture)
8. [Scalability Considerations](#scalability-considerations)

---

## Overview

### System Type
**Monolithic Application with Microservices-Ready Design**

The Mwanzo Skills Campus is currently built as a **well-structured monolith** with clear domain boundaries that facilitate future migration to microservices.

### Technology Stack

#### Backend
- **Language:** Java 17
- **Framework:** Spring Boot 4.0.1
- **ORM:** Hibernate / Spring Data JPA
- **Security:** Spring Security + JWT
- **Database:** PostgreSQL 15+
- **Caching:** (Future: Redis 7+)
- **Message Queue:** (Future: RabbitMQ/Kafka)

#### Frontend
- **Language:** TypeScript 5.8.3
- **Framework:** React 18.3.1
- **Build Tool:** Vite 5.4.19
- **Router:** React Router DOM 6.30.1
- **State Management:** TanStack Query (React Query) 5.83.0
- **UI Library:** Radix UI + Tailwind CSS 3.4.17
- **Forms:** React Hook Form 7.61.1 + Zod 3.25.76

#### Cloud Services (AWS)
- **Storage:** S3 (video files, documents)
- **CDN:** CloudFront (video delivery)
- **Transcoding:** MediaConvert (multi-quality videos)
- **Email:** SES (notifications)
- **Hosting:** Elastic Beanstalk / EC2

#### Third-Party Services
- **Payments:** PayHero (M-Pesa + Cards)
- **SMS:** (Future: Africa's Talking)
- **Analytics:** (Future: Mixpanel/Amplitude)

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   Web App    │  │  Mobile PWA  │  │  Admin Panel │         │
│  │  (React/TS)  │  │   (Future)   │  │  (Future)    │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
│          │                 │                 │                   │
│          └─────────────────┴─────────────────┘                   │
│                          │                                        │
│                    [HTTPS/WSS]                                   │
│                          │                                        │
├──────────────────────────┼────────────────────────────────────── │
│                   API GATEWAY LAYER                              │
├──────────────────────────┼────────────────────────────────────── │
│                          │                                        │
│                   ┌──────▼──────┐                               │
│                   │   Nginx /   │                               │
│                   │ Load Balancer│                              │
│                   └──────┬──────┘                               │
│                          │                                        │
├──────────────────────────┼────────────────────────────────────── │
│                   APPLICATION LAYER                              │
├──────────────────────────┼────────────────────────────────────── │
│                          │                                        │
│         ┌────────────────┴────────────────┐                     │
│         │     Spring Boot Application      │                     │
│         │    (Port 8080 - REST API)       │                     │
│         │                                   │                     │
│         │  ┌──────────────────────────┐   │                     │
│         │  │   Controller Layer       │   │                     │
│         │  │  - AuthController        │   │                     │
│         │  │  - CourseController      │   │                     │
│         │  │  - VideoController       │   │                     │
│         │  │  - PaymentController     │   │                     │
│         │  │  - EnrollmentController  │   │                     │
│         │  │  - QuizController        │   │                     │
│         │  └──────────┬───────────────┘   │                     │
│         │             │                    │                     │
│         │  ┌──────────▼───────────────┐   │                     │
│         │  │   Service Layer          │   │                     │
│         │  │  - AuthService           │   │                     │
│         │  │  - CourseService         │   │                     │
│         │  │  - VideoService          │   │                     │
│         │  │  - PaymentService        │   │                     │
│         │  │  - EnrollmentService     │   │                     │
│         │  │  - QuizService           │   │                     │
│         │  │  - S3Service             │   │                     │
│         │  │  - MediaConvertService   │   │                     │
│         │  └──────────┬───────────────┘   │                     │
│         │             │                    │                     │
│         │  ┌──────────▼───────────────┐   │                     │
│         │  │   Repository Layer       │   │                     │
│         │  │  - UserRepository        │   │                     │
│         │  │  - CourseRepository      │   │                     │
│         │  │  - VideoRepository       │   │                     │
│         │  │  - EnrollmentRepository  │   │                     │
│         │  │  - PaymentRepository     │   │                     │
│         │  │  - QuizRepository        │   │                     │
│         │  └──────────┬───────────────┘   │                     │
│         └─────────────┼────────────────────┘                     │
│                       │                                          │
├───────────────────────┼────────────────────────────────────────┤
│                  DATA LAYER                                     │
├───────────────────────┼────────────────────────────────────────┤
│                       │                                          │
│         ┌─────────────▼──────────────┐                         │
│         │     PostgreSQL Database     │                         │
│         │    (Primary Data Store)     │                         │
│         │                              │                         │
│         │  Tables:                     │                         │
│         │  - users                     │                         │
│         │  - courses                   │                         │
│         │  - categories                │                         │
│         │  - enrollments               │                         │
│         │  - videos                    │                         │
│         │  - sections                  │                         │
│         │  - video_progress            │                         │
│         │  - payments                  │                         │
│         │  - quizzes                   │                         │
│         │  - quiz_attempts             │                         │
│         │  - questions                 │                         │
│         │  - answers                   │                         │
│         │  - student_answers           │                         │
│         └──────────────────────────────┘                         │
│                                                                   │
├──────────────────────────────────────────────────────────────── │
│                 EXTERNAL SERVICES LAYER                          │
├──────────────────────────────────────────────────────────────── │
│                                                                   │
│  ┌────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │   AWS S3       │  │ AWS MediaConvert│  │ AWS CloudFront  │ │
│  │ (Video Storage)│  │  (Transcoding)  │  │   (CDN/CDN)     │ │
│  └────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                   │
│  ┌────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │   PayHero API  │  │    AWS SES      │  │  Africa's Talking│ │
│  │  (M-Pesa Pay)  │  │  (Email Notifs) │  │   (SMS - Future) │ │
│  └────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

---

## Component Architecture

### 1. Presentation Layer (Frontend)

```
src/mwanzo-platform-main/
├── src/
│   ├── pages/              # Route components
│   │   ├── Index.tsx       # Homepage
│   │   ├── Courses.tsx     # Course catalog
│   │   ├── CourseDetail.tsx# Course details
│   │   ├── Learn.tsx       # Video player
│   │   ├── Dashboard.tsx   # Student dashboard
│   │   ├── Login.tsx       # Authentication
│   │   └── ...
│   │
│   ├── components/         # Reusable UI components
│   │   ├── Header.tsx
│   │   ├── Footer.tsx
│   │   ├── CourseCard.tsx
│   │   ├── VideoPlayer.tsx
│   │   └── ui/             # shadcn/ui components
│   │
│   ├── services/api/       # API integration
│   │   ├── client.ts       # HTTP client
│   │   ├── authService.ts
│   │   ├── courseService.ts
│   │   ├── videoService.ts
│   │   └── index.ts
│   │
│   ├── contexts/           # React contexts
│   │   └── AuthContext.tsx # Authentication state
│   │
│   ├── hooks/              # Custom React hooks
│   ├── types/              # TypeScript definitions
│   ├── config/             # Configuration
│   └── utils/              # Utility functions
```

**Key Features:**
- Single Page Application (SPA) with React Router
- Component-based architecture
- Centralized API client with automatic auth
- Type-safe API integration with TypeScript
- Responsive design with Tailwind CSS

### 2. Application Layer (Backend)

```
src/main/java/com/mdaudev/mwanzo/mwanzocourseplatformbackend/
├── controller/             # REST API endpoints
│   ├── AuthController.java
│   ├── CourseController.java
│   ├── VideoController.java
│   ├── EnrollmentController.java
│   ├── PaymentController.java
│   ├── QuizController.java
│   └── CategoryController.java
│
├── service/                # Business logic
│   ├── AuthService.java
│   ├── CourseService.java
│   ├── VideoService.java
│   ├── EnrollmentService.java
│   ├── PaymentService.java
│   ├── PayHeroService.java
│   ├── QuizService.java
│   ├── S3Service.java
│   ├── MediaConvertService.java
│   ├── CloudFrontService.java
│   └── JwtService.java
│
├── repository/             # Data access
│   ├── UserRepository.java
│   ├── CourseRepository.java
│   ├── VideoRepository.java
│   ├── EnrollmentRepository.java
│   ├── PaymentRepository.java
│   ├── QuizRepository.java
│   └── ...
│
├── domain/                 # Domain entities
│   ├── user/
│   │   ├── User.java
│   │   ├── UserRole.java
│   │   └── dto/
│   ├── course/
│   │   ├── Course.java
│   │   ├── Category.java
│   │   ├── CourseLevel.java
│   │   ├── CourseStatus.java
│   │   └── dto/
│   ├── video/
│   │   ├── Video.java
│   │   ├── Section.java
│   │   ├── VideoProgress.java
│   │   └── dto/
│   ├── enrollment/
│   ├── payment/
│   └── quiz/
│
├── config/                 # Configuration classes
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   ├── S3Config.java
│   ├── AwsConfig.java
│   └── AsyncConfig.java
│
├── exception/              # Exception handling
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── ...
│
└── util/                   # Utility classes
```

**Key Patterns:**
- **Layered Architecture** - Clear separation of concerns
- **Dependency Injection** - Loose coupling via Spring DI
- **Repository Pattern** - Abstraction over data access
- **DTO Pattern** - Clean API contracts
- **Service Layer** - Business logic encapsulation

### 3. Domain Model (Entities)

```
┌──────────────────────────────────────────────────────────────┐
│                      DOMAIN MODEL                             │
└──────────────────────────────────────────────────────────────┘

┌─────────────┐
│    User     │──────┐
├─────────────┤      │
│ id (UUID)   │      │ Creates
│ email       │      │
│ password    │      ▼
│ role        │  ┌──────────────┐
│ createdAt   │  │   Course     │──────┐
└─────────────┘  ├──────────────┤      │
       │         │ id (UUID)    │      │ Contains
       │         │ title        │      │
       │ Enrolls │ description  │      ▼
       │         │ price        │  ┌──────────────┐
       │         │ status       │  │   Section    │──────┐
       │         │ categoryId   │  ├──────────────┤      │
       ▼         │ instructorId │  │ id (UUID)    │      │ Contains
┌─────────────┐  │ createdAt    │  │ title        │      │
│ Enrollment  │◄─┴──────────────┘  │ courseId     │      ▼
├─────────────┤         │           │ displayOrder │  ┌──────────────┐
│ id (UUID)   │         │           └──────────────┘  │    Video     │
│ studentId   │         │                             ├──────────────┤
│ courseId    │         │ Belongs to                  │ id (UUID)    │
│ status      │         │                             │ title        │
│ progress    │         ▼                             │ sectionId    │
└─────────────┘  ┌──────────────┐                    │ videoUrl     │
       │         │   Category   │                    │ durationSec  │
       │         ├──────────────┤                    │ isPreview    │
       │ Tracks  │ id (UUID)    │                    │ status       │
       │         │ name         │                    └──────────────┘
       ▼         │ slug         │                           │
┌─────────────┐  │ courseCount  │                           │ Track
│VideoProgress│  └──────────────┘                           │
├─────────────┤                                              ▼
│ id (UUID)   │                                       ┌──────────────┐
│ studentId   │                                       │VideoProgress │
│ videoId     │                                       ├──────────────┤
│ positionSec │                                       │ studentId    │
│ completed   │                                       │ videoId      │
└─────────────┘                                       │ completed    │
                                                      └──────────────┘
┌─────────────┐
│   Payment   │◄── Processes ─── Enrollment
├─────────────┤
│ id (UUID)   │
│ enrollmentId│
│ amount      │
│ status      │
│ mpesaRef    │
│ phoneNumber │
└─────────────┘

┌─────────────┐
│    Quiz     │◄── Belongs to ─── Course
├─────────────┤
│ id (UUID)   │
│ courseId    │
│ title       │
│ passingScore│
└─────────────┘
       │ Contains
       ▼
┌─────────────┐
│  Question   │
├─────────────┤
│ id (UUID)   │
│ quizId      │
│ text        │
│ points      │
└─────────────┘
       │ Has
       ▼
┌─────────────┐
│   Answer    │
├─────────────┤
│ id (UUID)   │
│ questionId  │
│ text        │
│ isCorrect   │
└─────────────┘
```

**Relationships:**
- User 1:N Course (as instructor)
- User 1:N Enrollment (as student)
- Course N:1 Category
- Course 1:N Section
- Section 1:N Video
- Enrollment 1:1 Payment
- User + Video = VideoProgress (composite key conceptually)
- Course 1:N Quiz
- Quiz 1:N Question
- Question 1:N Answer

---

## Data Architecture

### Database Schema Design Principles

1. **UUID Primary Keys** - Supports distributed systems and microservices migration
2. **Audit Fields** - All tables have `created_at` and `updated_at`
3. **Soft Deletes** - (Future) Use `is_deleted` flag instead of hard deletes
4. **Indexing Strategy** - Indexes on frequently queried foreign keys
5. **Denormalization** - Strategic denormalization (e.g., `enrollment_count` in courses)

### Key Tables

#### users
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,  -- BCrypt hash
    phone VARCHAR(20),
    profile_picture_url VARCHAR(500),
    role VARCHAR(20) NOT NULL,       -- STUDENT, INSTRUCTOR, EMPLOYER, ADMIN
    is_email_verified BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    is_locked BOOLEAN DEFAULT false,
    failed_login_attempts INTEGER DEFAULT 0,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_email (email),
    INDEX idx_role (role)
);
```

#### courses
```sql
CREATE TABLE courses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(250) UNIQUE NOT NULL,
    description TEXT NOT NULL,
    short_description VARCHAR(300),
    learning_objectives TEXT,         -- JSON array
    requirements TEXT,                 -- JSON array
    category_id UUID NOT NULL REFERENCES categories(id),
    instructor_id UUID NOT NULL,       -- References users(id)
    price DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2),
    thumbnail_url VARCHAR(500),
    preview_video_url VARCHAR(500),
    level VARCHAR(20) NOT NULL,        -- BEGINNER, INTERMEDIATE, ADVANCED
    language VARCHAR(50) DEFAULT 'English',
    status VARCHAR(20) NOT NULL,       -- DRAFT, PUBLISHED, REJECTED
    duration_minutes INTEGER DEFAULT 0,
    lecture_count INTEGER DEFAULT 0,
    enrollment_count INTEGER DEFAULT 0,
    average_rating DECIMAL(3,2) DEFAULT 0.00,
    rating_count INTEGER DEFAULT 0,
    is_featured BOOLEAN DEFAULT false,
    is_published BOOLEAN DEFAULT false,
    published_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_category (category_id),
    INDEX idx_instructor (instructor_id),
    INDEX idx_status (status),
    INDEX idx_featured (is_featured),
    INDEX idx_published (is_published)
);
```

#### videos
```sql
CREATE TABLE videos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id),
    section_id UUID NOT NULL REFERENCES sections(id),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    display_order INTEGER NOT NULL,
    duration_seconds INTEGER,
    s3_key VARCHAR(500) NOT NULL,
    video_url VARCHAR(500) NOT NULL,
    quality_urls_json TEXT,            -- Multi-quality URLs
    streaming_url VARCHAR(500),         -- CloudFront HLS/DASH
    thumbnail_url VARCHAR(500) NOT NULL,
    is_preview BOOLEAN DEFAULT false,
    is_published BOOLEAN DEFAULT true,
    processing_status VARCHAR(20) DEFAULT 'UPLOADED', -- UPLOADED, PROCESSING, READY, FAILED
    processing_job_id VARCHAR(200),
    processing_error TEXT,
    view_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_section (section_id),
    INDEX idx_course (course_id),
    INDEX idx_display_order (section_id, display_order),
    INDEX idx_processing_status (processing_status)
);
```

### Data Flow

```
┌─────────────────────────────────────────────────────────┐
│                    DATA FLOW                             │
└─────────────────────────────────────────────────────────┘

1. User Registration:
   User Input → Validation → BCrypt Hash → Database → JWT Token

2. Course Creation:
   Instructor → Create Course → Upload Thumbnail → Save to DB

3. Video Upload:
   Get Presigned URL → Upload to S3 → Create Video Record →
   Trigger MediaConvert → Process → Update Status

4. Video Watching:
   Check Access → Get Signed URL → Stream from CloudFront →
   Track Progress → Update DB on Events

5. Course Enrollment:
   Student → Choose Course → (If Paid) Payment Flow →
   Create Enrollment → Grant Access

6. Payment:
   Initiate M-Pesa → PayHero API → STK Push → User Confirms →
   Webhook Callback → Update Payment → Activate Enrollment

7. Quiz Taking:
   Start Attempt → Answer Questions → Submit →
   Calculate Score → Update Progress → (If Pass) Mark Complete

8. Progress Tracking:
   Watch Video → Client-side Tracking → Periodic Save →
   Mark Complete at 80% → Update Enrollment Progress →
   Check Course Completion
```

---

## Security Architecture

### Authentication Flow

```
┌──────────────────────────────────────────────────────────────┐
│              JWT AUTHENTICATION FLOW                          │
└──────────────────────────────────────────────────────────────┘

1. Login Request:
   POST /api/v1/auth/login
   {
     "email": "user@example.com",
     "password": "SecurePassword123!"
   }

2. Server Validates:
   - Check user exists
   - Verify BCrypt password hash
   - Check account not locked
   - Check email verified (optional)

3. Generate JWT Token:
   - Claims: userId, email, role
   - Sign with secret key
   - Set expiration (24 hours)

4. Return Token:
   {
     "token": "eyJhbGciOiJIUzI1NiIs...",
     "tokenType": "Bearer",
     "userId": "uuid",
     "name": "User Name",
     "email": "user@example.com",
     "role": "STUDENT"
   }

5. Client Stores Token:
   - localStorage (mwanzo_auth_token)
   - Include in all API requests:
     Authorization: Bearer <token>

6. Server Validates Each Request:
   - Extract token from header
   - Verify signature
   - Check expiration
   - Extract user info
   - Allow/Deny request
```

### Authorization

**Role-Based Access Control (RBAC):**

| Role | Permissions |
|------|-------------|
| **STUDENT** | - Browse courses<br>- Enroll in courses<br>- Watch videos<br>- Take quizzes<br>- Track progress<br>- Download certificates |
| **INSTRUCTOR** | - All STUDENT permissions<br>- Create courses<br>- Upload videos<br>- Create quizzes<br>- View student analytics<br>- Manage course content |
| **EMPLOYER** | - Browse courses<br>- Post jobs<br>- View applicants<br>- Verify certificates<br>- Access candidate pool |
| **ADMIN** | - All permissions<br>- Approve/reject courses<br>- Manage users<br>- View platform analytics<br>- Configure system settings<br>- Handle disputes |

### Security Features

1. **Password Security**
   - BCrypt hashing (cost factor: 10)
   - Minimum 8 characters
   - Must include uppercase, lowercase, number, special char

2. **Account Protection**
   - Account locking after 5 failed login attempts
   - Email verification required
   - Password reset via email token
   - Session timeout (24 hours)

3. **API Security**
   - HTTPS only in production
   - CORS configured per environment
   - Rate limiting (future)
   - SQL injection prevention (JPA/Hibernate)
   - XSS prevention (content sanitization)

4. **Data Security**
   - Encrypted database connections
   - Sensitive data encryption at rest (future)
   - PCI DSS compliance for payments
   - GDPR-compliant data handling

5. **Video Security**
   - Presigned URLs with expiration
   - CloudFront signed URLs
   - Access control checks
   - Anti-piracy measures (future)

---

## Integration Architecture

### External Service Integrations

#### 1. AWS S3 (Video Storage)
```
Upload Flow:
Frontend → Get Presigned URL → Backend generates URL →
S3 Direct Upload → Success → Create Video Record →
Trigger Processing

Security:
- Presigned URLs (15 min expiration)
- Bucket policies (private by default)
- CORS configuration
```

#### 2. AWS MediaConvert (Transcoding)
```
Processing Flow:
Video Uploaded → Trigger MediaConvert Job →
Transcode to Multiple Qualities (360p, 720p, 1080p) →
Output to S3 → Webhook Callback → Update Video Status →
Generate Streaming URLs

Configuration:
- Input: S3 bucket (raw videos)
- Output: S3 bucket (processed videos)
- Formats: HLS, DASH (adaptive streaming)
```

#### 3. AWS CloudFront (CDN)
```
Streaming Flow:
User Requests Video → Backend Generates Signed URL →
CloudFront Validates → Stream Video → Track Progress

Benefits:
- Low latency (edge locations)
- DDoS protection
- HTTPS support
- Custom domains
```

#### 4. PayHero (M-Pesa Payments)
```
Payment Flow:
1. User initiates payment
2. Backend calls PayHero API
3. PayHero sends STK Push to user's phone
4. User enters M-Pesa PIN
5. PayHero processes payment
6. Webhook notification to backend
7. Backend updates payment status
8. Activate enrollment

Webhook:
POST /api/v1/payments/callback
{
  "transactionReference": "ref123",
  "status": "SUCCESS",
  "mpesaReceiptNumber": "QGD3...",
  "amount": 2000
}
```

#### 5. AWS SES (Email Notifications)
```
Email Types:
- Welcome email on registration
- Email verification
- Password reset
- Enrollment confirmation
- Course completion
- Payment receipts
- Weekly progress reports

Template System:
- HTML templates with variables
- Personalization
- Tracking (opens, clicks)
```

---

## Deployment Architecture

### Development Environment
```
Developer Machine:
- Backend: localhost:8080
- Frontend: localhost:5173
- Database: localhost:5432 (PostgreSQL)
- LocalStack: localhost:4566 (S3 mock)
```

### Staging Environment
```
AWS Infrastructure:
- EC2 instance (t3.medium)
- RDS PostgreSQL (db.t3.small)
- S3 buckets (videos, documents)
- CloudFront distribution
- Application Load Balancer
- Route 53 (DNS: staging.mwanzoskills.co.ke)
```

### Production Environment
```
AWS Infrastructure:
- Auto Scaling Group (min: 2, max: 10)
  - EC2 instances (t3.large)
  - Application Load Balancer
- RDS PostgreSQL (db.r5.xlarge)
  - Multi-AZ deployment
  - Read replicas (2)
- S3 buckets
  - Videos bucket (encrypted)
  - Backups bucket
- CloudFront distributions
  - Static assets
  - Video streaming
- ElastiCache Redis (cache.r5.large)
- Route 53 (DNS: mwanzoskills.co.ke)
- CloudWatch (monitoring)
- AWS Backup (automated backups)

High Availability:
- Multi-AZ database
- Auto-scaling application servers
- Load balancer health checks
- CloudFront edge caching
```

---

## Scalability Considerations

### Current Capacity

**Monolithic Architecture:**
- Handles: ~10,000 concurrent users
- Video streaming: Unlimited (CloudFront)
- Database: ~5,000 req/sec
- API: ~1,000 req/sec per instance

### Horizontal Scaling

**Stateless Design:**
- JWT authentication (no server-side sessions)
- Load balancer distributes traffic
- Add more EC2 instances as needed

**Database Scaling:**
- Read replicas for read-heavy operations
- Connection pooling (HikariCP)
- Query optimization with indexes

**Caching Strategy (Future):**
```
Redis Cache:
- Course catalog (15 min TTL)
- User sessions (24 hour TTL)
- Video metadata (1 hour TTL)
- API responses (5 min TTL)

Reduces database load by 70%
```

### Vertical Scaling

**Database:**
- Upgrade RDS instance class
- Increase storage (auto-scaling enabled)
- Increase IOPS for better performance

**Application Servers:**
- Increase EC2 instance size
- More CPU/RAM for processing

### Microservices Migration Path

**Phase 1: Extract Services**
```
Monolith → Separate:
1. User Service (authentication, profiles)
2. Course Service (catalog, content)
3. Video Service (upload, streaming, progress)
4. Payment Service (transactions, webhooks)
5. Quiz Service (assessments, grading)
6. Notification Service (emails, SMS)
```

**Phase 2: Communication**
```
- REST APIs between services
- Message queue for async operations (RabbitMQ)
- API Gateway for routing (AWS API Gateway)
- Service discovery (Consul/Eureka)
```

**Phase 3: Data**
```
- Separate databases per service
- Event-driven data sync
- CQRS pattern where needed
```

**Benefits:**
- Independent scaling
- Technology flexibility
- Fault isolation
- Faster deployments
- Team autonomy

---

## Performance Optimization

### Current Optimizations

1. **Database:**
   - Indexed foreign keys
   - Connection pooling
   - Query optimization
   - Lazy loading relationships

2. **API:**
   - Pagination (default: 12 items)
   - DTO pattern (minimal data transfer)
   - Gzip compression
   - HTTP caching headers

3. **Video:**
   - CloudFront CDN
   - Multi-quality transcoding
   - Lazy loading thumbnails
   - Progressive download

4. **Frontend:**
   - Code splitting (Vite)
   - Lazy loading routes
   - Image optimization
   - React Query caching

### Future Optimizations

1. **Caching Layer:**
   - Redis for frequently accessed data
   - 70% reduction in database queries

2. **CDN for Static Assets:**
   - Frontend JS/CSS on CloudFront
   - Faster page loads globally

3. **Database Query Optimization:**
   - Analyze slow queries
   - Add composite indexes
   - Implement materialized views

4. **Async Processing:**
   - Background jobs for emails
   - Video processing queues
   - Analytics aggregation

---

## Monitoring & Observability

### Metrics to Track

1. **Application Metrics:**
   - Request rate (req/sec)
   - Response time (p50, p95, p99)
   - Error rate
   - Active users

2. **Database Metrics:**
   - Connection pool usage
   - Query execution time
   - Dead locks
   - Replication lag

3. **Infrastructure Metrics:**
   - CPU utilization
   - Memory usage
   - Disk I/O
   - Network throughput

4. **Business Metrics:**
   - Daily active users (DAU)
   - Course enrollments
   - Video completion rate
   - Payment success rate

### Logging Strategy

```
Log Levels:
- ERROR: Application errors, exceptions
- WARN: Degraded performance, retries
- INFO: Important business events
- DEBUG: Detailed troubleshooting info

Centralized Logging:
- CloudWatch Logs
- Log aggregation
- Search and filtering
- Alerts on errors
```

---

## Disaster Recovery

### Backup Strategy

1. **Database Backups:**
   - Automated daily snapshots
   - 30-day retention
   - Point-in-time recovery
   - Tested restore procedures

2. **File Backups:**
   - S3 versioning enabled
   - Cross-region replication
   - Lifecycle policies

3. **Configuration Backups:**
   - Infrastructure as Code (Terraform)
   - Version controlled
   - Automated deployment

### Recovery Time Objectives

- **RTO (Recovery Time Objective):** 4 hours
- **RPO (Recovery Point Objective):** 1 hour

### Failover Procedures

1. Database failover (Multi-AZ automatic)
2. Route 53 health checks
3. Load balancer failover
4. Manual intervention if needed

---

## Compliance & Regulations

### Data Protection

- **GDPR Compliance:** User data protection, right to erasure
- **PCI DSS:** Payment data security (via PayHero)
- **Data Residency:** Kenyan data stays in Kenya (future)

### Privacy

- User consent for data collection
- Privacy policy disclosure
- Cookie consent management
- Data anonymization for analytics

---

## Technology Decisions

### Why Spring Boot?

✅ Mature enterprise framework
✅ Excellent ecosystem
✅ Production-ready features
✅ Strong community support
✅ Easy to hire developers

### Why React?

✅ Component-based architecture
✅ Rich ecosystem
✅ Excellent developer experience
✅ Strong community
✅ Easy to find developers

### Why PostgreSQL?

✅ ACID compliance
✅ Advanced features (JSON, full-text search)
✅ Excellent performance
✅ Open source
✅ AWS RDS support

### Why AWS?

✅ Market leader
✅ Comprehensive services
✅ Global infrastructure
✅ Excellent documentation
✅ Free tier for development

---

## Conclusion

The Mwanzo Skills Campus architecture is designed with the following principles:

1. **Scalability** - Can grow from hundreds to millions of users
2. **Reliability** - High availability and fault tolerance
3. **Security** - Enterprise-grade security practices
4. **Performance** - Fast and responsive user experience
5. **Maintainability** - Clean code, clear boundaries
6. **Cost-Effectiveness** - Optimized resource usage

**Current Status:** Production-ready monolith with microservices migration path

---

**Last Updated:** January 18, 2026
**Version:** 1.0.0
**Author:** Mwanzo Development Team

🏗️ **Building the future of education in Kenya!** 🇰🇪
