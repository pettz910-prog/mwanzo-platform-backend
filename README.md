# 🎓 Mwanzo Skills Campus Platform

> **Full-stack e-learning platform connecting students, instructors, and employers in Kenya**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3.1-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.8.3-blue.svg)](https://www.typescriptlang.org/)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)](./LICENSE)

---

## 🚀 Quick Start

Get the full platform running in **15 minutes**:

```bash
# 1. Clone repository
git clone https://github.com/your-org/mwanzo-course-platform-backend.git
cd mwanzo-course-platform-backend

# 2. Start services
docker-compose up -d

# 3. Run backend
./mvnw spring-boot:run

# 4. Run frontend (in new terminal)
cd src/mwanzo-platform-main
npm install && npm run dev
```

**Access:**
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- API Docs: http://localhost:8080/swagger-ui.html

📖 **Detailed setup:** [Quick Start Guide](./docs/QUICK_START.md)

---

## 📋 What's Included

### Core Features
✅ **Authentication** - JWT-based auth with role-based access (Student, Instructor, Employer, Admin)
✅ **Course Management** - Create, publish, and manage courses with sections & videos
✅ **Video Streaming** - AWS S3 + MediaConvert + CloudFront CDN integration
✅ **M-Pesa Payments** - Seamless mobile money integration via PayHero
✅ **Quiz System** - Assessments with anti-cheat measures
✅ **Progress Tracking** - Real-time learning progress and completion tracking
✅ **Job Matching** - Connect students with relevant job opportunities

### Technology Stack

**Backend:**
- Java 17 + Spring Boot 4.0.1
- PostgreSQL 15 (primary database)
- AWS S3, MediaConvert, CloudFront
- JWT authentication
- Maven build system

**Frontend:**
- React 18 + TypeScript 5
- Vite (build tool)
- TailwindCSS + Radix UI
- React Query (state management)
- React Router (navigation)

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| **[Quick Start](./docs/QUICK_START.md)** | 15-minute setup guide |
| **[System Architecture](./docs/architecture/SYSTEM_ARCHITECTURE.md)** | High-level design and components |
| **[API Reference](./docs/api/API_REFERENCE.md)** | Complete API documentation (35+ endpoints) |
| **[Frontend Integration](./docs/integration/FRONTEND_INTEGRATION.md)** | React integration guide |
| **[Deployment Guide](./docs/deployment/DEPLOYMENT.md)** | AWS + Vercel deployment |
| **[Testing Guide](./docs/testing/TESTING.md)** | Testing strategy and examples |
| **[Environment Setup](./docs/ENVIRONMENT_SETUP.md)** | AWS credentials and configuration |

📚 **[Complete Documentation Index](./DOCUMENTATION_INDEX.md)**

---

## 🛠️ Development

### Prerequisites
- Java 17+
- Node.js 18+
- PostgreSQL 15+
- Docker Desktop (optional, for LocalStack)
- AWS Account (for production)

### Backend Development
```bash
# Run with hot reload
./mvnw spring-boot:run

# Run tests
./mvnw test

# Build JAR
./mvnw clean package
```

### Frontend Development
```bash
cd src/mwanzo-platform-main

# Install dependencies
npm install

# Dev server with hot reload
npm run dev

# Build for production
npm run build

# Type check
npm run type-check
```

---

## 🗄️ Database Schema

**8 Core Tables:**
- `users` - Authentication & profiles
- `categories` - Course categories
- `courses` - Course catalog
- `sections` - Course modules
- `videos` - Video lessons
- `enrollments` - Student enrollments
- `payments` - Payment transactions
- `video_progress` - Learning progress

**Additional Tables:** `quizzes`, `questions`, `answers`, `quiz_attempts`, `student_answers`

📊 **[Complete Schema Documentation](./docs/architecture/DATABASE_SCHEMA.md)**

---

## 🔐 Security

- **Authentication:** JWT tokens (24-hour expiry)
- **Authorization:** Role-based access control (RBAC)
- **Passwords:** BCrypt hashing (cost factor: 10)
- **API Security:** HTTPS, CORS, rate limiting
- **Video Security:** Presigned URLs, CloudFront signed URLs
- **Payment Security:** PCI DSS compliant via PayHero

---

## 🚀 Deployment

### Production Architecture
```
Frontend (Vercel) → API Gateway (AWS) → Backend (ECS/EC2)
                                      ↓
                           RDS PostgreSQL (Multi-AZ)
                                      ↓
               S3 (Videos) → MediaConvert → CloudFront (CDN)
```

**Deploy Backend:**
```bash
# Build Docker image
docker build -t mwanzo-backend .

# Deploy to AWS
# See deployment guide for details
```

**Deploy Frontend:**
```bash
cd src/mwanzo-platform-main
vercel --prod
```

📦 **[Complete Deployment Guide](./docs/deployment/DEPLOYMENT.md)**

---

## 🧪 Testing

```bash
# Backend tests
./mvnw test                    # Unit tests
./mvnw verify                  # Integration tests

# Frontend tests
cd src/mwanzo-platform-main
npm test                       # Unit tests
npm run test:e2e              # E2E tests (Cypress)
```

**Test Coverage Goal:** >80%

---

## 📊 Project Status

**Current Version:** 1.0.0 (Production-Ready)

**Completed Features:**
- ✅ User authentication & authorization
- ✅ Course CRUD operations
- ✅ Video upload & streaming
- ✅ M-Pesa payment integration
- ✅ Quiz system
- ✅ Progress tracking
- ✅ Responsive UI

**In Progress:**
- 🔄 Certificate generation
- 🔄 Email notifications (AWS SES)
- 🔄 Job platform features

**Planned Features:**
- ⏳ Live streaming classes
- ⏳ Discussion forums
- ⏳ Mobile apps (iOS/Android)
- ⏳ Analytics dashboard

---

## 🤝 Contributing

We follow standard Git workflow:

1. Create feature branch: `git checkout -b feature/my-feature`
2. Make changes and commit: `git commit -m "Add feature"`
3. Push to remote: `git push origin feature/my-feature`
4. Create Pull Request

**Code Standards:**
- Follow Java naming conventions
- Use meaningful variable names
- Write comprehensive JavaDoc
- Add unit tests for new features
- Update documentation

---

## 📞 Support

- **Documentation:** [Complete Docs](./DOCUMENTATION_INDEX.md)
- **Issues:** [GitHub Issues](https://github.com/your-org/mwanzo-platform/issues)
- **Email:** dev@mwanzoskills.co.ke
- **Slack:** #mwanzo-dev

---

## 📄 License

**Proprietary Software** - All rights reserved.

Copyright © 2026 Mwanzo Skills Campus. Unauthorized copying, modification, distribution, or use of this software is strictly prohibited.

---

## 🙏 Acknowledgments

Built with ❤️ for the Kenyan education ecosystem.

**Key Technologies:**
- [Spring Boot](https://spring.io/projects/spring-boot)
- [React](https://reactjs.org/)
- [PostgreSQL](https://www.postgresql.org/)
- [AWS](https://aws.amazon.com/)
- [PayHero](https://payhero.co.ke/)

---

**Made in Kenya 🇰🇪 | Transforming Education**
