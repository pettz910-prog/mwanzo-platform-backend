# 🚀 Quick Start Guide - Mwanzo Skills Campus

> **Get the full platform running in 15 minutes**

---

## Prerequisites Checklist

Before starting, ensure you have:

- [ ] **Java 17+** installed ([Download OpenJDK](https://adoptium.net/))
- [ ] **Node.js 18+** installed ([Download](https://nodejs.org/))
- [ ] **PostgreSQL 15+** installed ([Download](https://www.postgresql.org/download/))
- [ ] **Maven 3.8+** installed (or use included mvnw)
- [ ] **Docker Desktop** installed (optional, for LocalStack)
- [ ] **Git** installed
- [ ] **IDE** - IntelliJ IDEA (recommended) or VS Code

---

## Part 1: Backend Setup (5 minutes)

### Step 1: Clone and Navigate
```bash
git clone https://github.com/your-org/mwanzo-course-platform-backend.git
cd mwanzo-course-platform-backend
```

### Step 2: Database Setup
```bash
# Create PostgreSQL database
psql -U postgres
CREATE DATABASE mwanzo_database;
\q
```

### Step 3: Configure Application
Edit `src/main/resources/application.yaml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mwanzo_database
    username: postgres
    password: YOUR_PASSWORD  # Change this!
```

### Step 4: Start LocalStack (Optional - for S3)
```bash
# If using Docker
docker-compose up -d localstack

# Or skip if you have AWS credentials
```

### Step 5: Run Backend
```bash
# Option 1: Using Maven wrapper
./mvnw spring-boot:run

# Option 2: Using IntelliJ
# Click the green ▶️ button next to MwanzoCoursePlatformBackendApplication.java
```

**Backend should now be running at:** `http://localhost:8080`

### Step 6: Verify Backend
```bash
curl http://localhost:8080/api/health
```

Expected response:
```json
{
  "status": "UP",
  "application": "Mwanzo Skills Campus",
  "timestamp": "2026-01-18T..."
}
```

✅ **Backend is ready!**

---

## Part 2: Frontend Setup (5 minutes)

### Step 1: Navigate to Frontend
```bash
cd src/mwanzo-platform-main
```

### Step 2: Install Dependencies
```bash
npm install
# or
bun install  # if using bun
```

### Step 3: Configure API URL
Create `.env.local`:
```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

### Step 4: Run Frontend
```bash
npm run dev
```

**Frontend should now be running at:** `http://localhost:5173`

### Step 5: Open in Browser
```bash
# macOS
open http://localhost:5173

# Windows
start http://localhost:5173

# Linux
xdg-open http://localhost:5173
```

✅ **Frontend is ready!**

---

## Part 3: Initial Data Setup (3 minutes)

### Option 1: Using Postman/Insomnia

Import the Postman collection from `./postman/Mwanzo-API.postman_collection.json`

### Option 2: Using cURL

#### Create a Category
```bash
curl -X POST http://localhost:8080/api/v1/categories \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Web Development",
    "description": "Learn to build modern websites and web applications",
    "icon": "💻"
  }'
```

#### Register a Test User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "password": "Password123!",
    "phone": "254712345678",
    "role": "STUDENT"
  }'
```

#### Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Password123!"
  }'
```

Save the `token` from the response - you'll need it for authenticated requests.

### Option 3: Using Automated Script

Run the data seeder:
```bash
# From backend root
./mvnw spring-boot:run -Dspring-boot.run.arguments="--seed-data=true"
```

This creates:
- 5 categories
- 10 sample courses
- 3 test users (student, instructor, admin)

✅ **Initial data loaded!**

---

## Part 4: Test the Full Flow (2 minutes)

### 1. Browse Courses
Open browser: `http://localhost:5173/courses`

You should see the course catalog with sample courses.

### 2. Register an Account
1. Click "Sign Up" in the header
2. Fill in the registration form
3. Submit

### 3. Login
1. Click "Login"
2. Enter your credentials
3. You should be redirected to the dashboard

### 4. Enroll in a Free Course
1. Find a free course (price = 0)
2. Click "Enroll Now"
3. Access should be granted immediately

### 5. Watch a Video
1. Go to "My Courses"
2. Click on your enrolled course
3. Start watching a video
4. Progress should be tracked automatically

✅ **Full platform is working!**

---

## Troubleshooting

### Backend Won't Start

**Error:** "Port 8080 already in use"
```bash
# Find and kill the process
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# macOS/Linux
lsof -ti:8080 | xargs kill -9
```

**Error:** "Could not connect to database"
```bash
# Check PostgreSQL is running
# Windows
sc query postgresql-x64-15

# macOS
brew services list | grep postgresql

# Linux
sudo systemctl status postgresql
```

**Error:** "Schema validation failed"
```bash
# Reset database
psql -U postgres
DROP DATABASE mwanzo_database;
CREATE DATABASE mwanzo_database;
\q

# Restart backend - Hibernate will recreate tables
```

### Frontend Won't Start

**Error:** "EADDRINUSE: address already in use"
```bash
# Kill the process on port 5173
# Windows
netstat -ano | findstr :5173
taskkill /PID <PID> /F

# macOS/Linux
lsof -ti:5173 | xargs kill -9
```

**Error:** "Module not found"
```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install
```

**Error:** "API calls failing (CORS)"
```bash
# Check backend CORS configuration in SecurityConfig.java
# Default allows http://localhost:5173
```

### LocalStack Issues

**Error:** "Cannot connect to LocalStack"
```bash
# Start LocalStack
docker-compose up -d localstack

# Check if running
docker ps | grep localstack

# Check logs
docker logs mwanzo-localstack
```

**Error:** "Bucket does not exist"
```bash
# LocalStack auto-creates buckets, but you can create manually:
aws --endpoint-url=http://localhost:4566 s3 mb s3://mwanzo-videos
```

---

## What's Next?

### For Students/Users
1. 📖 Read **[User Guide](./user-guides/STUDENT_GUIDE.md)** to learn all features
2. 🎓 Explore courses and start learning
3. 📱 Try the mobile-responsive UI

### For Developers
1. 📚 Read **[System Architecture](./architecture/SYSTEM_ARCHITECTURE.md)** to understand the design
2. 🔧 Check **[Development Setup](./DEVELOPMENT_SETUP.md)** for advanced configuration
3. 🧪 Run tests: `./mvnw test` (backend) and `npm test` (frontend)
4. 📝 Review **[Code Best Practices](./best-practices/CODE_PRACTICES.md)**

### For Instructors
1. 📖 Read **[Instructor Guide](./user-guides/INSTRUCTOR_GUIDE.md)**
2. 🎬 Learn how to upload videos
3. 💰 Understand revenue sharing

### For System Administrators
1. 🚀 Read **[Deployment Guide](./deployment/DEPLOYMENT_OVERVIEW.md)**
2. 🔐 Configure **[Security Settings](./architecture/SECURITY.md)**
3. 📊 Setup **[Monitoring](./operations/MONITORING.md)**

---

## Default Credentials (Seeded Data)

If you ran the data seeder, these accounts are available:

| Role | Email | Password | Purpose |
|------|-------|----------|---------|
| Student | student@test.com | Student123! | Test student features |
| Instructor | instructor@test.com | Instructor123! | Test course creation |
| Admin | admin@test.com | Admin123! | Test admin features |

**⚠️ IMPORTANT:** Change these credentials in production!

---

## Quick Commands Reference

### Backend
```bash
# Start backend
./mvnw spring-boot:run

# Run tests
./mvnw test

# Build JAR
./mvnw clean package

# Run with profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend
```bash
# Start dev server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Run linter
npm run lint
```

### Docker
```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f

# Restart a service
docker-compose restart postgres
```

### Database
```bash
# Connect to database
psql -U postgres -d mwanzo_database

# Backup database
pg_dump -U postgres mwanzo_database > backup.sql

# Restore database
psql -U postgres -d mwanzo_database < backup.sql

# Reset database
DROP DATABASE mwanzo_database;
CREATE DATABASE mwanzo_database;
```

---

## System Requirements

### Minimum Requirements
- **CPU:** 2 cores
- **RAM:** 4 GB
- **Storage:** 10 GB free space
- **OS:** Windows 10+, macOS 11+, or Linux

### Recommended Requirements
- **CPU:** 4+ cores
- **RAM:** 8 GB
- **Storage:** 20 GB SSD
- **OS:** Latest version

---

## Support & Help

### Documentation
- 📚 **[Full Documentation Index](../DOCUMENTATION_INDEX.md)**
- 🏗️ **[Architecture Docs](./architecture/)**
- 🔧 **[API Reference](./api/API_REFERENCE.md)**

### Getting Help
- 💬 **Slack:** #mwanzo-dev channel
- 📧 **Email:** dev@mwanzoskills.co.ke
- 🐛 **Issues:** GitHub Issues

### Resources
- 🎥 **Video Tutorials:** [Coming Soon]
- 📖 **Blog Posts:** [Coming Soon]
- 🎓 **Developer Training:** [Coming Soon]

---

**Congratulations! 🎉 You're now running the complete Mwanzo Skills Campus platform!**

---

**Total Setup Time:** ~15 minutes
**Last Updated:** January 18, 2026
**Version:** 1.0.0

🎓 **Happy Coding!** 🚀
