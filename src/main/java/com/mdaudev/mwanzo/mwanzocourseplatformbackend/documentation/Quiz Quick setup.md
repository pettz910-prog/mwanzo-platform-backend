# 🚀 Quiz System - Quick Setup Checklist

## ✅ Step-by-Step Setup

### Step 1: Copy Files (5 minutes)

**Entities → domain.course:**
- [ ] Quiz.java
- [ ] Question.java
- [ ] QuestionType.java
- [ ] Answer.java
- [ ] QuizAttempt.java
- [ ] AttemptStatus.java
- [ ] StudentAnswer.java

**Repositories → repository:**
- [ ] QuizRepository.java
- [ ] QuestionRepository.java
- [ ] AnswerRepository.java
- [ ] QuizAttemptRepository.java
- [ ] StudentAnswerRepository.java

**Services → service:**
- [ ] QuizService.java

**Controllers → controller:**
- [ ] QuizController.java

---

### Step 2: Restart Application (1 minute)

```bash
# In IntelliJ: Stop → Run
# Or terminal:
./mvnw spring-boot:run
```

**Look for:**
```
✅ Hibernate: create table quizzes...
✅ Hibernate: create table questions...
✅ Hibernate: create table answers...
✅ Hibernate: create table quiz_attempts...
✅ Hibernate: create table student_answers...
✅ Started MwanzoCourseApplication
```

---

### Step 3: Verify Database (2 minutes)

```sql
-- Connect to PostgreSQL
psql -U postgres -d mwanzo_database

-- List all tables
\dt

-- Should show NEW tables:
-- quizzes
-- questions
-- answers
-- quiz_attempts
-- student_answers

-- Check structure
\d quizzes
\d questions
```

---

### Step 4: Test API (5 minutes)

**Test 1: Create Quiz**
```bash
curl -X POST http://localhost:8080/api/v1/quizzes \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": "YOUR_COURSE_ID",
    "title": "Python Basics Quiz",
    "description": "Test your knowledge",
    "passingScore": 70,
    "displayOrder": 1
  }'
```

**Test 2: Add Question**
```bash
curl -X POST http://localhost:8080/api/v1/quizzes/QUIZ_ID/questions \
  -H "Content-Type: application/json" \
  -d '{
    "questionText": "What is 2 + 2?",
    "displayOrder": 1,
    "points": 10
  }'
```

**Test 3: Add Correct Answer**
```bash
curl -X POST http://localhost:8080/api/v1/quizzes/questions/QUESTION_ID/answers \
  -H "Content-Type: application/json" \
  -d '{
    "answerText": "4",
    "isCorrect": true,
    "displayOrder": 1
  }'
```

**Test 4: Add Wrong Answers**
```bash
# Repeat 3 times with different wrong answers:
curl -X POST http://localhost:8080/api/v1/quizzes/questions/QUESTION_ID/answers \
  -H "Content-Type: application/json" \
  -d '{
    "answerText": "3",
    "isCorrect": false,
    "displayOrder": 2
  }'
```

**Test 5: Start Quiz**
```bash
curl -X POST http://localhost:8080/api/v1/quizzes/QUIZ_ID/start \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "YOUR_STUDENT_ID"
  }'
```

---

## 🎯 Complete Test Scenario

### Create a Full Quiz:

1. **Create Quiz:** "Python Basics" (3 questions)

2. **Question 1:** "What is 2 + 2?"
    - ✅ 4 (correct)
    - ❌ 3
    - ❌ 5
    - ❌ 22

3. **Question 2:** "What prints 'Hello'?"
    - ✅ print("Hello") (correct)
    - ❌ echo "Hello"
    - ❌ console.log("Hello")
    - ❌ System.out.println("Hello")

4. **Question 3:** "Python file extension?"
    - ✅ .py (correct)
    - ❌ .python
    - ❌ .pt
    - ❌ .pyc

5. **Start Quiz** (student takes quiz)

6. **Answer all questions**

7. **Submit Quiz** → Should get 100% if all correct!

---

## ✅ Quick Verification

After setup, verify:

- [ ] 14 files added successfully
- [ ] No compilation errors
- [ ] Application starts
- [ ] 5 new database tables created
- [ ] Can create quiz via API
- [ ] Can add questions
- [ ] Can add answers
- [ ] Can start quiz
- [ ] Can submit quiz
- [ ] Grading works automatically

---

## 🎉 Success Indicators

**You'll know it's working when:**

1. ✅ No errors in console
2. ✅ Database tables created
3. ✅ API endpoints respond
4. ✅ Quiz creation works
5. ✅ Quiz taking works
6. ✅ Auto-grading works
7. ✅ Course completion triggered

---

## 🆘 Troubleshooting

### Issue: "Cannot find symbol Quiz"
**Fix:** Check Quiz.java is in domain.course package

### Issue: "Cannot find symbol QuizRepository"
**Fix:** Check QuizRepository.java is in repository package

### Issue: Tables not created
**Fix:** Check application.yml has:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

### Issue: API returns 404
**Fix:** Check QuizController.java is in controller package

---

## 📊 Expected Results

### Database:
```
Total tables: 13
New tables: 5 (quizzes, questions, answers, quiz_attempts, student_answers)
```

### API:
```
New endpoints: 11
Total endpoints: 50+
```

### Features:
```
✅ Quiz creation
✅ Question management
✅ Answer management
✅ Quiz taking
✅ Automatic grading
✅ Attempt tracking
✅ Course completion
```

---

## 🎯 You're Done When...

✅ All 14 files copied
✅ Application restarts successfully
✅ 5 database tables created
✅ Created test quiz
✅ Added test questions
✅ Student can take quiz
✅ Grading works automatically

---

**Total Setup Time: 10-15 minutes**
**Difficulty: Easy** ✨
**Result: Complete Quiz System** 🎓

---

**Ready to test?** Follow this checklist step by step!

**Need help?** See QUIZ_SYSTEM_COMPLETE.md for detailed docs.

🚀 **Let's go!**