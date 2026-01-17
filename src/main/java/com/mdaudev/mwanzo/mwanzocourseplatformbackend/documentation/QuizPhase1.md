# Quiz System - Phase 1: Domain Models Complete ✅

## 🎯 What We Just Created

### Entities (7 files):

1. **Quiz.java** - Quiz assessment entity
    - Belongs to course/section
    - Configurable: time limit, passing score, max attempts
    - Anti-cheat: shuffle questions/answers
    - Required for course completion

2. **Question.java** - Quiz questions
    - Multiple choice (single answer)
    - Points system
    - Optional explanation
    - Image support

3. **QuestionType.java** - Question type enum
    - MULTIPLE_CHOICE (implemented)
    - MULTIPLE_SELECT (future)
    - TRUE_FALSE (future)
    - SHORT_ANSWER (future)

4. **Answer.java** - Question answer options
    - Typically 4 options per question
    - One marked as correct
    - Optional explanation

5. **QuizAttempt.java** - Student quiz submissions
    - Tracks score, time, status
    - Pass/fail logic
    - Anti-cheat: IP, user agent tracking
    - Timeout handling

6. **AttemptStatus.java** - Attempt status enum
    - IN_PROGRESS
    - SUBMITTED
    - ABANDONED
    - FLAGGED (anti-cheat)

7. **StudentAnswer.java** - Individual answer records
    - Links attempt → question → selected answer
    - Tracks correctness
    - Points calculation

---

## 📊 Database Schema

### New Tables:

**quizzes:**
```sql
- id (UUID PK)
- course_id (UUID FK)
- section_id (UUID FK, nullable)
- title, description
- time_limit_minutes (nullable)
- passing_score (default 70)
- max_attempts (nullable)
- question_count
- is_published, is_required
- shuffle_questions, shuffle_answers
- show_correct_answers
```

**questions:**
```sql
- id (UUID PK)
- quiz_id (UUID FK)
- question_text
- question_type (enum)
- display_order
- points (default 1)
- explanation (nullable)
- image_url (nullable)
```

**answers:**
```sql
- id (UUID PK)
- question_id (UUID FK)
- answer_text
- is_correct (boolean)
- display_order
- explanation (nullable)
```

**quiz_attempts:**
```sql
- id (UUID PK)
- student_id (UUID FK)
- enrollment_id (UUID FK)
- quiz_id (UUID FK)
- attempt_number
- score, points_earned, total_points
- correct_answers, total_questions
- is_passed
- status (enum)
- started_at, submitted_at
- time_taken_seconds
- ip_address, user_agent (anti-cheat)
```

**student_answers:**
```sql
- id (UUID PK)
- attempt_id (UUID FK)
- question_id (UUID FK)
- selected_answer_id (UUID FK)
- is_correct
- points_earned
- answered_at
- UNIQUE(attempt_id, question_id)
```

---

## 🎯 Key Features

### 1. Flexible Configuration
- ✅ Time limits (optional)
- ✅ Passing scores (customizable)
- ✅ Attempt limits (optional/unlimited)
- ✅ Required/optional quizzes

### 2. Anti-Cheat Measures
- ✅ Shuffle questions per attempt
- ✅ Shuffle answer options
- ✅ IP address tracking
- ✅ User agent tracking
- ✅ Time limit enforcement
- ✅ Attempt limit enforcement

### 3. Grading System
- ✅ Points-based scoring
- ✅ Percentage calculation
- ✅ Pass/fail determination
- ✅ Automatic grading

### 4. Student Experience
- ✅ See correct answers after submission (optional)
- ✅ Explanations for answers
- ✅ Multiple attempts (if allowed)
- ✅ Resume capability (in progress attempts)

---

## 🚀 Next Steps (Phase 2)

Create repositories and services:
1. ⏳ QuizRepository
2. ⏳ QuestionRepository
3. ⏳ AnswerRepository
4. ⏳ QuizAttemptRepository
5. ⏳ StudentAnswerRepository
6. ⏳ QuizService (business logic)
7. ⏳ QuizController (REST API)

---

## 📦 Files Ready

All 7 files copied to outputs:
- Quiz.java
- Question.java
- QuestionType.java
- Answer.java
- QuizAttempt.java
- AttemptStatus.java
- StudentAnswer.java

**Add to:** `domain.course` package

**Restart app** to create database tables!

---

**Phase 1 Complete!** Ready for Phase 2? 🚀