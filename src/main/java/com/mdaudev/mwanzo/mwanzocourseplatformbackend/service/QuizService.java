package com.mdaudev.mwanzo.mwanzocourseplatformbackend.service;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.*;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.exception.ResourceNotFoundException;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Quiz Service
 *
 * Manages quizzes, questions, attempts, and grading.
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-13
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    public final QuizAttemptRepository quizAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;

    // ==========================================
    // QUIZ MANAGEMENT
    // ==========================================

    public List<Quiz> getPublishedQuizzes(UUID courseId) {
        return quizRepository.findPublishedQuizzesByCourseId(courseId);
    }

    public Quiz getQuizById(UUID quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
    }

    @Transactional
    public Quiz createQuiz(UUID courseId, UUID sectionId, String title, String description,
                           Integer timeLimitMinutes, Integer passingScore, Integer maxAttempts,
                           Integer displayOrder) {
        log.info("Creating quiz for course: {}", courseId);

        courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        Quiz quiz = Quiz.builder()
                .courseId(courseId)
                .sectionId(sectionId)
                .title(title)
                .description(description)
                .timeLimitMinutes(timeLimitMinutes)
                .passingScore(passingScore != null ? passingScore : 70)
                .maxAttempts(maxAttempts)
                .displayOrder(displayOrder)
                .build();

        Quiz saved = quizRepository.save(quiz);
        log.info("Quiz created: {}", saved.getId());
        return saved;
    }

    // ==========================================
    // QUESTION MANAGEMENT
    // ==========================================

    public List<Question> getQuizQuestions(UUID quizId, boolean shuffle) {
        List<Question> questions = questionRepository.findActiveQuestionsByQuizId(quizId);

        if (shuffle) {
            Collections.shuffle(questions);
        }

        return questions;
    }

    @Transactional
    public Question createQuestion(UUID quizId, String questionText, Integer displayOrder, Integer points) {
        log.info("Creating question for quiz: {}", quizId);

        Quiz quiz = getQuizById(quizId);

        Question question = Question.builder()
                .quizId(quizId)
                .questionText(questionText)
                .displayOrder(displayOrder)
                .points(points != null ? points : 1)
                .build();

        Question saved = questionRepository.save(question);

        quiz.addQuestion();
        quizRepository.save(quiz);

        log.info("Question created: {}", saved.getId());
        return saved;
    }

    // ==========================================
    // ANSWER MANAGEMENT
    // ==========================================

    public List<Answer> getQuestionAnswers(UUID questionId, boolean shuffle) {
        List<Answer> answers = answerRepository.findByQuestionIdOrderByDisplayOrderAsc(questionId);

        if (shuffle) {
            Collections.shuffle(answers);
        }

        return answers;
    }

    @Transactional
    public Answer createAnswer(UUID questionId, String answerText, Boolean isCorrect, Integer displayOrder) {
        log.info("Creating answer for question: {}", questionId);

        questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));

        Answer answer = Answer.builder()
                .questionId(questionId)
                .answerText(answerText)
                .isCorrect(isCorrect != null ? isCorrect : false)
                .displayOrder(displayOrder)
                .build();

        Answer saved = answerRepository.save(answer);
        log.info("Answer created: {}", saved.getId());
        return saved;
    }

    // ==========================================
    // QUIZ ATTEMPT MANAGEMENT
    // ==========================================

    /**
     * Start a new quiz attempt.
     * Checks enrollment, attempt limits, and creates attempt record.
     */
    @Transactional
    public QuizAttempt startQuizAttempt(UUID studentId, UUID quizId, String ipAddress, String userAgent) {
        log.info("Student {} starting quiz {}", studentId, quizId);

        Quiz quiz = getQuizById(quizId);

        // Check if student is enrolled
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(studentId, quiz.getCourseId())
                .orElseThrow(() -> new IllegalStateException("Student not enrolled in course"));

        if (!enrollment.isAccessible()) {
            throw new IllegalStateException("Enrollment not active");
        }

        // Check for existing in-progress attempt
        var inProgressAttempt = quizAttemptRepository.findInProgressAttempt(
                studentId, quizId, AttemptStatus.IN_PROGRESS);

        if (inProgressAttempt.isPresent()) {
            QuizAttempt existing = inProgressAttempt.get();

            // Check if timed out
            if (existing.hasTimedOut(quiz.getTimeLimitMinutes())) {
                existing.setStatus(AttemptStatus.ABANDONED);
                quizAttemptRepository.save(existing);
                log.warn("Previous attempt {} timed out", existing.getId());
            } else {
                log.info("Returning existing in-progress attempt: {}", existing.getId());
                return existing;
            }
        }

        // Check attempt limit
        long attemptCount = quizAttemptRepository.countAttemptsByStudentAndQuiz(studentId, quizId);
        if (quiz.hasAttemptLimit() && attemptCount >= quiz.getMaxAttempts()) {
            throw new IllegalStateException(
                    String.format("Maximum attempts (%d) reached for this quiz", quiz.getMaxAttempts()));
        }

        // Create new attempt
        QuizAttempt attempt = QuizAttempt.builder()
                .studentId(studentId)
                .enrollmentId(enrollment.getId())
                .quizId(quizId)
                .courseId(quiz.getCourseId())
                .attemptNumber((int) (attemptCount + 1))
                .totalQuestions(quiz.getQuestionCount())
                .totalPoints(questionRepository.getTotalPointsByQuizId(quizId))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        QuizAttempt saved = quizAttemptRepository.save(attempt);
        log.info("Quiz attempt created: {} (attempt #{})", saved.getId(), saved.getAttemptNumber());
        return saved;
    }

    /**
     * Answer a quiz question.
     */
    @Transactional
    public StudentAnswer answerQuestion(UUID attemptId, UUID questionId, UUID selectedAnswerId) {
        log.debug("Recording answer - attempt: {}, question: {}, answer: {}",
                attemptId, questionId, selectedAnswerId);

        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("QuizAttempt", "id", attemptId));

        if (!attempt.isInProgress()) {
            throw new IllegalStateException("Quiz attempt is not in progress");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));

        Answer selectedAnswer = answerRepository.findById(selectedAnswerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer", "id", selectedAnswerId));

        // Check if already answered (allow update)
        StudentAnswer studentAnswer = studentAnswerRepository
                .findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElse(StudentAnswer.builder()
                        .attemptId(attemptId)
                        .questionId(questionId)
                        .build());

        studentAnswer.setSelectedAnswerId(selectedAnswerId);
        studentAnswer.setIsCorrect(selectedAnswer.getIsCorrect());
        studentAnswer.setPointsEarned(selectedAnswer.getIsCorrect() ? question.getPoints() : 0);

        return studentAnswerRepository.save(studentAnswer);
    }

    /**
     * Submit quiz and calculate final score.
     */
    @Transactional
    public QuizAttempt submitQuiz(UUID attemptId) {
        log.info("Submitting quiz attempt: {}", attemptId);

        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("QuizAttempt", "id", attemptId));

        if (!attempt.isInProgress()) {
            throw new IllegalStateException("Quiz attempt already submitted");
        }

        Quiz quiz = getQuizById(attempt.getQuizId());

        // Calculate results
        List<StudentAnswer> answers = studentAnswerRepository.findByAttemptId(attemptId);

        int correctAnswers = (int) answers.stream().filter(StudentAnswer::getIsCorrect).count();
        int pointsEarned = answers.stream()
                .mapToInt(StudentAnswer::getPointsEarned)
                .sum();

        // Submit attempt
        attempt.submit(
                correctAnswers,
                attempt.getTotalQuestions(),
                pointsEarned,
                attempt.getTotalPoints(),
                quiz.getPassingScore()
        );

        QuizAttempt submitted = quizAttemptRepository.save(attempt);

        log.info("Quiz submitted - Score: {}%, Passed: {}", submitted.getScore(), submitted.getIsPassed());

        // Update enrollment quiz completion
        if (submitted.getIsPassed()) {
            updateEnrollmentQuizCompletion(attempt.getEnrollmentId());
        }

        return submitted;
    }

    /**
     * Update enrollment quiz completion status.
     */
    @Transactional
    public void updateEnrollmentQuizCompletion(UUID enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", enrollmentId));

        long passedQuizzes = quizAttemptRepository.countPassedQuizzesByEnrollmentId(enrollmentId);
        long requiredQuizzes = quizRepository.findRequiredQuizzesByCourseId(enrollment.getCourseId()).size();

        if (requiredQuizzes > 0 && passedQuizzes >= requiredQuizzes) {
            enrollment.setQuizzesCompleted(true);

            // Check if videos also completed
            if (enrollment.getVideosCompleted()) {
                enrollment.markAsCompleted();
                log.info("🎉 Student {} completed course {}!",
                        enrollment.getStudentId(), enrollment.getCourseId());
            }

            enrollmentRepository.save(enrollment);
        }
    }

    /**
     * Get student's attempt history for a quiz.
     */
    public List<QuizAttempt> getStudentAttempts(UUID studentId, UUID quizId) {
        return quizAttemptRepository.findByStudentIdAndQuizIdOrderByAttemptNumberDesc(studentId, quizId);
    }

    /**
     * Get student's answers for an attempt.
     */
    public List<StudentAnswer> getAttemptAnswers(UUID attemptId) {
        return studentAnswerRepository.findByAttemptId(attemptId);
    }

    /**
     * Check if student can start/retake quiz.
     */
    public boolean canStartQuiz(UUID studentId, UUID quizId) {
        Quiz quiz = getQuizById(quizId);

        // Check enrollment
        var enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, quiz.getCourseId());
        if (enrollment.isEmpty() || !enrollment.get().isAccessible()) {
            return false;
        }

        // Check attempt limit
        if (quiz.hasAttemptLimit()) {
            long attempts = quizAttemptRepository.countAttemptsByStudentAndQuiz(studentId, quizId);
            return attempts < quiz.getMaxAttempts();
        }

        return true;
    }
}