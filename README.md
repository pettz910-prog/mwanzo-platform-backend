# Mwanzo Skills Campus Platform - Backend API

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-brightgreen.svg)
![Java](https://img.shields.io/badge/Java-17-orange.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue.svg)
![License](https://img.shields.io/badge/License-Proprietary-red.svg)

> A comprehensive e-learning and job placement platform designed specifically for the Kenyan market.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
    - [Configuration](#configuration)
    - [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Development Guidelines](#development-guidelines)
- [Testing](#testing)
- [Deployment](#deployment)
- [Contributing](#contributing)
- [License](#license)

---

## 🎯 Overview

**Mwanzo Skills Campus** is a full-featured online learning platform that bridges the gap between skills training and employment in Kenya. The platform enables:

- **Students** to learn valuable skills through professional courses and get matched with job opportunities
- **Instructors** to create and monetize comprehensive courses (70% revenue share)
- **Employers** to post jobs and access a pool of pre-trained, certified candidates

Think of it as **"Udemy meets LinkedIn Jobs"** - specifically tailored for the Kenyan market with M-Pesa integration, local job focus, and certificate verification.

---

## ✨ Key Features

### For Students
- 📚 Browse and enroll in professional courses
- 🎥 HD video streaming with anti-skip protection
- 📝 Interactive quizzes with anti-cheat measures
- 🎓 Verifiable certificates with QR codes
- 💼 Automatic job matching based on completed courses
- 💳 Payment via M-Pesa or card

### For Instructors
- 🎬 Upload comprehensive course content
- 💰 Earn 70% of every course sale
- 📊 Real-time analytics and earnings dashboard
- 💸 Automated payouts every 3 weeks
- ⭐ Student reviews and ratings

### For Employers
- 📢 Post job openings (free initially)
- 👥 Access pre-trained candidates
- ✅ Verify certificates directly
- 📬 Get notified when qualified candidates complete relevant courses
- 🔍 Filter applicants by completed courses

### Platform Features
- 🔐 JWT-based authentication
- 🛡️ Role-based access control (Student, Instructor, Employer, Admin)
- 📧 Email notifications (AWS SES)
- 🔔 Real-time in-app notifications
- 📈 Comprehensive analytics
- 🎯 Anti-cheat and anti-piracy protection
- ☁️ AWS S3 video storage with CloudFront CDN

---

## 🛠️ Technology Stack

### Backend Framework
- **Spring Boot 4.0.1** - Application framework
- **Java 17** - Programming language
- **Maven** - Dependency management

### Databases
- **PostgreSQL 15+** - Primary relational database
- **Redis 7+** (Future) - Caching and session management
- **MongoDB 7+** (Future) - Analytics and logs

### Security
- **Spring Security** - Authentication and authorization
- **JWT** - Stateless authentication tokens
- **BCrypt** - Password hashing

### Cloud Services (AWS)
- **S3** - Video and file storage
- **CloudFront** - Content delivery network
- **MediaConvert** - Video transcoding
- **SES** - Email service

### Payment Gateways
- **PayHero** - Payment processing (M-Pesa + Cards)
- **M-Pesa Daraja API** (Future) - Direct M-Pesa integration

### Additional Libraries
- **Lombok** - Reduces boilerplate code
- **Hibernate** - ORM
- **Jackson** - JSON processing
- **Validation API** - Request validation

---

## 🏗️ Architecture

### Monolithic Architecture (Current)
The application follows a **domain-driven design** with clear separation of concerns:

```
com.mdaudev.mwanzo.mwanzocourseplatformbackend
├── config              # Configuration classes
├── controller          # REST API endpoints
├── domain              # Domain entities
│   ├── course          # Course domain
│   ├── user            # User domain (future)
│   ├── payment         # Payment domain (future)
│   └── job             # Job domain (future)
├── repository          # Data access layer
├── service             # Business logic
├── exception           # Custom exceptions
└── dto                 # Data Transfer Objects
```

### Design Principles
- ✅ **Single Responsibility** - Each class has one clear purpose
- ✅ **Dependency Injection** - Loose coupling via Spring DI
- ✅ **Repository Pattern** - Abstraction over data access
- ✅ **DTO Pattern** - Clean API contracts
- ✅ **Service Layer** - Business logic separation
- ✅ **Exception Handling** - Global error handling

### Future Migration Path
The codebase is designed with **clear domain boundaries** to facilitate future extraction into microservices:
- User Service
- Course Service
- Payment Service
- Job Service
- Video Service
- Notification Service

---

## 🚀 Getting Started

### Prerequisites

Ensure you have the following installed:

- **Java 17** or higher ([Download](https://adoptium.net/))
- **Maven 3.8+** ([Download](https://maven.apache.org/download.cgi))
- **PostgreSQL 15+** ([Download](https://www.postgresql.org/download/))
- **IntelliJ IDEA Ultimate** (recommended) or any Java IDE
- **Postman** or **Insomnia** (for API testing)
- **Git** ([Download](https://git-scm.com/downloads))

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-org/mwanzo-course-platform-backend.git
   cd mwanzo-course-platform-backend
   ```

2. **Create PostgreSQL database**
   ```sql
   CREATE DATABASE mwanzo_database;
   ```

3. **Configure application properties**

   Navigate to `src/main/resources/application.yml` and update:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/mwanzo_database
       username: your_postgres_username
       password: your_postgres_password
   ```

4. **Install dependencies**
   ```bash
   mvn clean install
   ```

### Configuration

#### Database Configuration
Update `application.yml` with your PostgreSQL credentials:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mwanzo_database
    username: postgres
    password: your_password
```

#### AWS Configuration (Optional - for production)
```yaml
aws:
  access-key: YOUR_AWS_ACCESS_KEY
  secret-key: YOUR_AWS_SECRET_KEY
  s3:
    bucket-name: mwanzo-course-content
  region: us-east-1
```

#### Email Configuration
```yaml
spring:
  mail:
    host: email-smtp.us-east-1.amazonaws.com
    username: YOUR_SMTP_USERNAME
    password: YOUR_SMTP_PASSWORD
```

### Running the Application

#### Option 1: Using Maven
```bash
mvn spring-boot:run
```

#### Option 2: Using IntelliJ IDEA
1. Open project in IntelliJ
2. Wait for Maven to download dependencies
3. Click the green ▶️ play button next to the main class
4. Or press `Shift + F10`

#### Option 3: Using JAR
```bash
mvn clean package
java -jar target/mwanzo-course-platform-backend-0.0.1-SNAPSHOT.jar
```

The application will start on **http://localhost:8080**

### Verify Installation

Test the health check endpoint:
```bash
curl http://localhost:8080/api/health
```

Expected response:
```json
{
  "status": "UP",
  "application": "Mwanzo Skills Campus Platform",
  "version": "1.0.0",
  "timestamp": "2026-01-09T15:00:00",
  "message": "Application is running successfully!"
}
```

---

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api/v1
```

### Authentication
Most endpoints require JWT authentication:
```
Authorization: Bearer <your-jwt-token>
```

### Available Endpoints (Day 1 - Course Catalog)

#### Categories
- `GET /api/v1/categories` - List all categories
- `GET /api/v1/categories/{id}` - Get category details
- `POST /api/v1/categories` - Create category (Admin only)

#### Courses
- `GET /api/v1/courses` - Browse/search courses (Public)
- `GET /api/v1/courses/{id}` - Get course details (Public)
- `POST /api/v1/courses` - Create course (Instructor only)

### Future Endpoints (Coming Soon)
- Authentication & User Management
- Course Enrollment
- Video Streaming
- Payments
- Jobs & Applications
- Certificates

Full API documentation will be available at: `http://localhost:8080/swagger-ui.html` (Coming soon)

---

## 🗄️ Database Schema

### Current Tables (Day 1)

#### Categories
```sql
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(500),
    slug VARCHAR(120) UNIQUE,
    course_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Courses
```sql
CREATE TABLE courses (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(250) UNIQUE,
    description TEXT NOT NULL,
    short_description VARCHAR(300),
    category_id UUID REFERENCES categories(id),
    instructor_id UUID NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    level VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    enrollment_count INTEGER DEFAULT 0,
    average_rating DECIMAL(3,2) DEFAULT 0.00,
    is_published BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Future Tables
- users
- enrollments
- video_content
- quizzes
- certificates
- jobs
- applications
- payments
- reviews

---

## 💻 Development Guidelines

### Code Style
- Follow **Java naming conventions**
- Use **meaningful variable names**
- Add **comprehensive JavaDoc comments**
- Maximum **line length: 120 characters**
- Use **Lombok** to reduce boilerplate

### Git Workflow
1. Create feature branch: `git checkout -b feature/course-enrollment`
2. Make changes and commit: `git commit -m "Add course enrollment logic"`
3. Push to remote: `git push origin feature/course-enrollment`
4. Create Pull Request for review

### Commit Message Format
```
feat: Add course search functionality
fix: Fix payment callback handling
docs: Update API documentation
refactor: Simplify video upload logic
test: Add unit tests for course service
```

### Package Organization
- **Entities** → `domain.{domain-name}`
- **DTOs** → `domain.{domain-name}.dto`
- **Repositories** → `repository`
- **Services** → `service`
- **Controllers** → `controller`
- **Config** → `config`

---

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Test Coverage
```bash
mvn clean test jacoco:report
```
View report at: `target/site/jacoco/index.html`

---

## 🚢 Deployment

### Production Checklist
- [ ] Update `application-prod.yml` with production database
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate`
- [ ] Configure AWS credentials
- [ ] Set up environment variables
- [ ] Enable HTTPS/SSL
- [ ] Configure CORS properly
- [ ] Set up logging and monitoring
- [ ] Enable rate limiting
- [ ] Configure backup strategy

### Docker Deployment (Coming Soon)
```bash
docker build -t mwanzo-backend .
docker run -p 8080:8080 mwanzo-backend
```

### AWS Elastic Beanstalk (Coming Soon)
Deployment guide will be provided.

---

## 🤝 Contributing

### Team Members
- **Lead Developer** - Backend Architecture
- **Frontend Developer** - React UI
- **DevOps Engineer** - Deployment & Infrastructure

### Development Process
1. Pick a task from project board
2. Create feature branch
3. Implement with tests
4. Submit Pull Request
5. Code review
6. Merge to main

---

## 📄 License

**Proprietary Software** - All rights reserved.

This software is the property of Mwanzo Skills Campus. Unauthorized copying, modification, distribution, or use of this software is strictly prohibited.

---

## 📞 Contact & Support

- **Email**: support@mwanzoskills.co.ke
- **Website**: https://www.mwanzoskills.co.ke
- **Documentation**: https://docs.mwanzoskills.co.ke

---

## 📊 Project Status

### Current Phase: Day 1-5 (Course Catalog)
- ✅ Project setup
- ✅ Database configuration
- ✅ Category entity
- ✅ Course entity
- 🔄 DTOs and repositories (in progress)
- ⏳ Service layer
- ⏳ REST controllers
- ⏳ Exception handling

### Upcoming Phases
- **Week 1**: Student flow (enrollment, video learning, quizzes, certificates)
- **Week 2**: Instructor flow, job flow, admin dashboard

---

## 🙏 Acknowledgments

Built with ❤️ for the Kenyan education and employment ecosystem.

**Technologies & Tools:**
- Spring Boot Team
- PostgreSQL Community
- AWS
- JetBrains IntelliJ IDEA

---

**Last Updated**: January 9, 2026  
**Version**: 1.0.0  
**Status**: Active Development