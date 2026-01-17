package com.mdaudev.mwanzo.mwanzocourseplatformbackend.service;



import com.mdaudev.mwanzo.mwanzocourseplatformbackend.controller.*;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizControllerTest {

    @Mock
    private QuizService quizService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private QuizController quizController;

    private UUID courseId;
    private UUID quizId;
    private UUID studentId;
    private UUID attemptId;
    private UUID questionId;
    private UUID answerId;

    private Quiz quiz;
    private QuizAttempt attempt;
    private Question question;
    private Answer answer;
    private StudentAnswer studentAnswer;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        quizId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        attemptId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        answerId = UUID.randomUUID();

        quiz = Quiz.builder()
                .id(quizId)
                .courseId(courseId)
                .title("Sample Quiz")
                .description("Desc")
                .displayOrder(1)
                .shuffleQuestions(true)
                .shuffleAnswers(true)
                .showCorrectAnswers(true)
                .isPublished(true)
                .build();

        attempt = QuizAttempt.builder()
                .id(attemptId)
                .quizId(quizId)
                .studentId(studentId)
                .score(0)
                .build();

        question = Question.builder()
                .id(questionId)
                .quizId(quizId)
                .questionText("What is Java?")
                .displayOrder(1)
                .points(5)
                .build();

        answer = Answer.builder()
                .id(answerId)
                .questionId(questionId)
                .answerText("A programming language")
                .isCorrect(true)
                .displayOrder(1)
                .build();

        studentAnswer = StudentAnswer.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .questionId(questionId)
                .selectedAnswerId(answerId)
                .isCorrect(true)
                .pointsEarned(5)
                .build();
    }

    // ==========================================
    // STUDENT ENDPOINTS
    // ==========================================

    @Test
    void getCourseQuizzes_returnsListFromService() {
        when(quizService.getPublishedQuizzes(courseId))
                .thenReturn(List.of(quiz));

        ResponseEntity<List<Quiz>> response = quizController.getCourseQuizzes(courseId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsExactly(quiz);

        verify(quizService).getPublishedQuizzes(courseId);
    }

    @Test
    void getQuiz_returnsQuizFromService() {
        when(quizService.getQuizById(quizId)).thenReturn(quiz);

        ResponseEntity<Quiz> response = quizController.getQuiz(quizId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(quiz);

        verify(quizService).getQuizById(quizId);
    }

    @Test
    void startQuiz_createsAttemptAndBuildsResponse() {
        StartQuizRequest request = new StartQuizRequest();
        request.setStudentId(studentId);

        when(httpServletRequest.getHeader("User-Agent")).thenReturn("JUnit");
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        when(quizService.startQuizAttempt(studentId, quizId, "127.0.0.1", "JUnit"))
                .thenReturn(attempt);
        when(quizService.getQuizById(quizId)).thenReturn(quiz);
        when(quizService.getQuizQuestions(quizId, true))
                .thenReturn(List.of(question));
        when(quizService.getQuestionAnswers(questionId, true))
                .thenReturn(List.of(answer));

        ResponseEntity<QuizAttemptResponse> response =
                quizController.startQuiz(quizId, request, httpServletRequest);

        QuizAttemptResponse body = response.getBody();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(body).isNotNull();
        assertThat(body.getAttempt()).isEqualTo(attempt);
        assertThat(body.getQuiz()).isEqualTo(quiz);
        assertThat(body.getQuestions()).hasSize(1);
//        assertThat(body.getQuestions().get(0).getQuestion()).isEqualTo(question);
//        assertThat(body.getQuestions().get(0).getAnswers()).containsExactly(answer);

        verify(quizService).startQuizAttempt(studentId, quizId, "127.0.0.1", "JUnit");
        verify(quizService).getQuizById(quizId);
        verify(quizService).getQuizQuestions(quizId, true);
        verify(quizService).getQuestionAnswers(questionId, true);
    }

    @Test
    void answerQuestion_delegatesToServiceAndReturnsStudentAnswer() {
        AnswerQuestionRequest request = new AnswerQuestionRequest();
        request.setAttemptId(attemptId);
        request.setSelectedAnswerId(answerId);

        when(quizService.answerQuestion(attemptId, questionId, answerId))
                .thenReturn(studentAnswer);

        ResponseEntity<StudentAnswer> response =
                quizController.answerQuestion(questionId, request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(studentAnswer);

        verify(quizService).answerQuestion(attemptId, questionId, answerId);
    }

    @Test
    void submitQuiz_returnsResultResponseFromServiceData() {
        when(quizService.submitQuiz(attemptId)).thenReturn(attempt);
        when(quizService.getQuizById(quizId)).thenReturn(quiz);
        when(quizService.getAttemptAnswers(attemptId))
                .thenReturn(List.of(studentAnswer));

        ResponseEntity<QuizResultResponse> response =
                quizController.submitQuiz(attemptId);

        QuizResultResponse body = response.getBody();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(body).isNotNull();
        assertThat(body.getAttempt()).isEqualTo(attempt);
        assertThat(body.getShowCorrectAnswers()).isTrue();
        assertThat(body.getStudentAnswers()).containsExactly(studentAnswer);

        verify(quizService).submitQuiz(attemptId);
        verify(quizService).getQuizById(quizId);
        verify(quizService).getAttemptAnswers(attemptId);
    }

    @Test
    void getStudentAttempts_returnsAttemptsFromService() {
        QuizAttempt attempt2 = QuizAttempt.builder()
                .id(UUID.randomUUID())
                .quizId(quizId)
                .studentId(studentId)
                .score(10)
                .build();

        when(quizService.getStudentAttempts(studentId, quizId))
                .thenReturn(List.of(attempt, attempt2));

        ResponseEntity<List<QuizAttempt>> response =
                quizController.getStudentAttempts(quizId, studentId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsExactly(attempt, attempt2);

        verify(quizService).getStudentAttempts(studentId, quizId);
    }

    // ==========================================
    // INSTRUCTOR ENDPOINTS
    // ==========================================

    @Test
    void createQuiz_delegatesToServiceAndReturnsCreatedQuiz() {
        CreateQuizRequest request = new CreateQuizRequest();
        request.setCourseId(courseId);
        request.setSectionId(UUID.randomUUID());
        request.setTitle("New Quiz");
        request.setDescription("Quiz desc");
        request.setTimeLimitMinutes(30);
        request.setPassingScore(70);
        request.setMaxAttempts(3);
        request.setDisplayOrder(1);

        when(quizService.createQuiz(
                request.getCourseId(),
                request.getSectionId(),
                request.getTitle(),
                request.getDescription(),
                request.getTimeLimitMinutes(),
                request.getPassingScore(),
                request.getMaxAttempts(),
                request.getDisplayOrder()
        )).thenReturn(quiz);

        ResponseEntity<Quiz> response = quizController.createQuiz(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(quiz);

        verify(quizService).createQuiz(
                request.getCourseId(),
                request.getSectionId(),
                request.getTitle(),
                request.getDescription(),
                request.getTimeLimitMinutes(),
                request.getPassingScore(),
                request.getMaxAttempts(),
                request.getDisplayOrder()
        );
    }

    @Test
    void createQuestion_delegatesToServiceAndReturnsQuestion() {
        CreateQuestionRequest request = new CreateQuestionRequest();
        request.setQuestionText("What is Java?");
        request.setDisplayOrder(1);
        request.setPoints(5);

        when(quizService.createQuestion(
                quizId,
                request.getQuestionText(),
                request.getDisplayOrder(),
                request.getPoints()
        )).thenReturn(question);

        ResponseEntity<Question> response =
                quizController.createQuestion(quizId, request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(question);

        verify(quizService).createQuestion(
                quizId,
                request.getQuestionText(),
                request.getDisplayOrder(),
                request.getPoints()
        );
    }

    @Test
    void createAnswer_delegatesToServiceAndReturnsAnswer() {
        CreateAnswerRequest request = new CreateAnswerRequest();
        request.setAnswerText("A language");
        request.setIsCorrect(true);
        request.setDisplayOrder(1);

        when(quizService.createAnswer(
                questionId,
                request.getAnswerText(),
                request.getIsCorrect(),
                request.getDisplayOrder()
        )).thenReturn(answer);

        ResponseEntity<Answer> response =
                quizController.createAnswer(questionId, request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(answer);

        verify(quizService).createAnswer(
                questionId,
                request.getAnswerText(),
                request.getIsCorrect(),
                request.getDisplayOrder()
        );
    }
}