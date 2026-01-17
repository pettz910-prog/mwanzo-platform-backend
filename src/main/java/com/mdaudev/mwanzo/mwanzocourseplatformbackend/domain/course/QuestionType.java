package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course;

/**
 * QuestionType Enum
 *
 * Defines the supported types of quiz questions.
 * Used to control how questions are rendered, answered, and graded.
 *
 * Stored as STRING in the database.
 *
 * Design goals:
 * - Simple today (MCQ)
 * - Extensible tomorrow (no schema changes)
 * - Works with existing QuizService logic
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-13
 */
public enum QuestionType {

    /**
     * Multiple choice question with ONE correct answer.
     *
     * Current default.
     * Used with Answer.isCorrect = true/false.
     */
    MULTIPLE_CHOICE(
            "Multiple Choice",
            false,
            true
    ),

    /**
     * Multiple choice question with MULTIPLE correct answers.
     *
     * Future:
     * - StudentAnswer will need multiple selected answers
     * - Partial scoring possible
     */
    MULTIPLE_SELECT(
            "Multiple Select",
            true,
            true
    ),

    /**
     * True or False question.
     *
     * Special case of multiple choice with two fixed answers.
     */
    TRUE_FALSE(
            "True / False",
            false,
            true
    ),

    /**
     * Short text answer.
     *
     * Future:
     * - Requires manual or AI-assisted grading
     * - No Answer table usage
     */
    SHORT_ANSWER(
            "Short Answer",
            false,
            false
    );

    private final String displayName;
    private final boolean allowsMultipleAnswers;
    private final boolean autoGradable;

    QuestionType(String displayName,
                 boolean allowsMultipleAnswers,
                 boolean autoGradable) {
        this.displayName = displayName;
        this.allowsMultipleAnswers = allowsMultipleAnswers;
        this.autoGradable = autoGradable;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean allowsMultipleAnswers() {
        return allowsMultipleAnswers;
    }

    public boolean isAutoGradable() {
        return autoGradable;
    }
}
