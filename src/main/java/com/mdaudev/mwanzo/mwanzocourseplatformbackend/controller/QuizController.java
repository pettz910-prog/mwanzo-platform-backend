package com.mdaudev.mwanzo.mwanzocourseplatformbackend.controller;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.*;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.service.QuizService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Quiz REST Controller
 *
 * Manages quiz creation, attempts, and grading.
 *
 * Base URL: /api/v1/quizzes
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-13
 */
@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
@Slf4j
public class QuizController {

    private final QuizService quizService;

    // ==========================================
    // STUDENT ENDPOINTS (Quiz Taking)
    // ==========================================

    /**
     * Get all quizzes for a course.
     *
     * @param courseId Course UUID
     * @return List of published quizzes
     */
    @GetMapping("/courses/{courseId}")
    public ResponseEntity<List<Quiz>> getCourseQuizzes(@PathVariable UUID courseId) {
        log.info("GET /api/v1/quizzes/courses/{}", courseId);

        List<Quiz> quizzes = quizService.getPublishedQuizzes(courseId);

        return ResponseEntity.ok(quizzes);
    }

    /**
     * Get quiz details.
     *
     * @param quizId Quiz UUID
     * @return Quiz details
     */
    @GetMapping("/{quizId}")
    public ResponseEntity<Quiz> getQuiz(@PathVariable UUID quizId) {
        log.info("GET /api/v1/quizzes/{}", quizId);

        Quiz quiz = quizService.getQuizById(quizId);

        return ResponseEntity.ok(quiz);
    }

    /**
     * Start a new quiz attempt.
     * Creates attempt record and returns quiz questions.
     *
     * Request:
     * {
     *   "studentId": "uuid"
     * }
     *
     * @param quizId Quiz UUID
     * @param request Start attempt request
     * @param httpRequest HTTP request (for IP/user agent)
     * @return Quiz attempt with questions
     */
    @PostMapping("/{quizId}/start")
    public ResponseEntity<QuizAttemptResponse> startQuiz(
            @PathVariable UUID quizId,
            @RequestBody StartQuizRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/v1/quizzes/{}/start - Student: {}", quizId, request.getStudentId());

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        QuizAttempt attempt = quizService.startQuizAttempt(
                request.getStudentId(),
                quizId,
                ipAddress,
                userAgent
        );

        Quiz quiz = quizService.getQuizById(quizId);
        List<Question> questions = quizService.getQuizQuestions(quizId, quiz.getShuffleQuestions());

        // Get answers for each question
        List<QuestionWithAnswers> questionsWithAnswers = questions.stream()
                .map(q -> {
                    List<Answer> answers = quizService.getQuestionAnswers(q.getId(), quiz.getShuffleAnswers());
                    return new QuestionWithAnswers(q, answers);
                })
                .toList();

        QuizAttemptResponse response = new QuizAttemptResponse(
                attempt,
                quiz,
                questionsWithAnswers
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Submit an answer to a question.
     * Can be called multiple times for same question (update answer).
     *
     * Request:
     * {
     *   "attemptId": "uuid",
     *   "selectedAnswerId": "uuid"
     * }
     *
     * @param questionId Question UUID
     * @param request Answer submission
     * @return Student answer record
     */
    @PostMapping("/questions/{questionId}/answer")
    public ResponseEntity<StudentAnswer> answerQuestion(
            @PathVariable UUID questionId,
            @RequestBody AnswerQuestionRequest request) {

        log.debug("POST /api/v1/quizzes/questions/{}/answer", questionId);

        StudentAnswer answer = quizService.answerQuestion(
                request.getAttemptId(),
                questionId,
                request.getSelectedAnswerId()
        );

        return ResponseEntity.ok(answer);
    }

    /**
     * Submit quiz for grading.
     * Calculates score and determines pass/fail.
     *
     * @param attemptId Quiz attempt UUID
     * @return Graded quiz attempt with results
     */
    @PostMapping("/attempts/{attemptId}/submit")
    public ResponseEntity<QuizResultResponse> submitQuiz(@PathVariable UUID attemptId) {
        log.info("POST /api/v1/quizzes/attempts/{}/submit", attemptId);

        QuizAttempt attempt = quizService.submitQuiz(attemptId);
        Quiz quiz = quizService.getQuizById(attempt.getQuizId());

        // Get answers with correct/incorrect details
        List<StudentAnswer> studentAnswers = quizService.getAttemptAnswers(attemptId);

        QuizResultResponse response = new QuizResultResponse(
                attempt,
                quiz.getShowCorrectAnswers(),
                studentAnswers
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get student's attempt history for a quiz.
     *
     * @param quizId Quiz UUID
     * @param studentId Student UUID
     * @return List of attempts
     */
    @GetMapping("/{quizId}/attempts")
    public ResponseEntity<List<QuizAttempt>> getStudentAttempts(
            @PathVariable UUID quizId,
            @RequestParam UUID studentId) {

        log.info("GET /api/v1/quizzes/{}/attempts?studentId={}", quizId, studentId);

        List<QuizAttempt> attempts = quizService.getStudentAttempts(studentId, quizId);

        return ResponseEntity.ok(attempts);
    }

    /**
     * Get attempt details with all answers.
     * Used to review previous attempt.
     *
     * @param attemptId Attempt UUID
     * @return Attempt with answers
     */
    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<AttemptDetailResponse> getAttemptDetails(@PathVariable UUID attemptId) {
        log.info("GET /api/v1/quizzes/attempts/{}", attemptId);

        QuizAttempt attempt = quizService.quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        List<StudentAnswer> answers = quizService.getAttemptAnswers(attemptId);
        Quiz quiz = quizService.getQuizById(attempt.getQuizId());

        AttemptDetailResponse response = new AttemptDetailResponse(
                attempt,
                answers,
                quiz.getShowCorrectAnswers()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Check if student can start/retake quiz.
     *
     * @param quizId Quiz UUID
     * @param studentId Student UUID
     * @return Eligibility status
     */
    @GetMapping("/{quizId}/can-start")
    public ResponseEntity<QuizEligibilityResponse> checkEligibility(
            @PathVariable UUID quizId,
            @RequestParam UUID studentId) {

        log.debug("GET /api/v1/quizzes/{}/can-start?studentId={}", quizId, studentId);

        boolean canStart = quizService.canStartQuiz(studentId, quizId);

        long attemptCount = quizService.quizAttemptRepository
                .countAttemptsByStudentAndQuiz(studentId, quizId);

        QuizEligibilityResponse response = new QuizEligibilityResponse(
                canStart,
                (int) attemptCount
        );

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // INSTRUCTOR ENDPOINTS (Quiz Management)
    // ==========================================

    /**
     * Create a new quiz.
     * TODO: Add @PreAuthorize("hasRole('INSTRUCTOR')")
     *
     * @param request Quiz creation request
     * @return Created quiz
     */
    @PostMapping
    public ResponseEntity<Quiz> createQuiz(@RequestBody CreateQuizRequest request) {
        log.info("POST /api/v1/quizzes - Creating quiz: {}", request.getTitle());

        Quiz quiz = quizService.createQuiz(
                request.getCourseId(),
                request.getSectionId(),
                request.getTitle(),
                request.getDescription(),
                request.getTimeLimitMinutes(),
                request.getPassingScore(),
                request.getMaxAttempts(),
                request.getDisplayOrder()
        );

        return ResponseEntity.status(201).body(quiz);
    }

    /**
     * Add a question to a quiz.
     * TODO: Add @PreAuthorize("hasRole('INSTRUCTOR')")
     *
     * @param quizId Quiz UUID
     * @param request Question creation request
     * @return Created question
     */
    @PostMapping("/{quizId}/questions")
    public ResponseEntity<Question> createQuestion(
            @PathVariable UUID quizId,
            @RequestBody CreateQuestionRequest request) {

        log.info("POST /api/v1/quizzes/{}/questions", quizId);

        Question question = quizService.createQuestion(
                quizId,
                request.getQuestionText(),
                request.getDisplayOrder(),
                request.getPoints()
        );

        return ResponseEntity.status(201).body(question);
    }

    /**
     * Add an answer to a question.
     * TODO: Add @PreAuthorize("hasRole('INSTRUCTOR')")
     *
     * @param questionId Question UUID
     * @param request Answer creation request
     * @return Created answer
     */
    @PostMapping("/questions/{questionId}/answers")
    public ResponseEntity<Answer> createAnswer(
            @PathVariable UUID questionId,
            @RequestBody CreateAnswerRequest request) {

        log.info("POST /api/v1/quizzes/questions/{}/answers", questionId);

        Answer answer = quizService.createAnswer(
                questionId,
                request.getAnswerText(),
                request.getIsCorrect(),
                request.getDisplayOrder()
        );

        return ResponseEntity.status(201).body(answer);
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}

// ==========================================
// REQUEST/RESPONSE DTOs
// ==========================================

@lombok.Data
@lombok.AllArgsConstructor
class QuestionWithAnswers {
    private Question question;
    private List<Answer> answers;
}

@lombok.Data
@lombok.AllArgsConstructor
class AttemptDetailResponse {
    private QuizAttempt attempt;
    private List<StudentAnswer> answers;
    private Boolean showCorrectAnswers;
}

@lombok.Data
@lombok.AllArgsConstructor
class QuizEligibilityResponse {
    private Boolean canStart;
    private Integer attemptCount;
}

