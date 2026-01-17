package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user;

/**
 * User Role Enum
 *
 * Defines all user roles in the system for role-based access control (RBAC).
 * Each role has specific permissions and access levels.
 *
 * Roles:
 * - STUDENT: Can browse courses, enroll, take quizzes, apply for jobs
 * - INSTRUCTOR: Can create courses, manage content, view earnings
 * - EMPLOYER: Can post jobs, view applications, hire candidates
 * - ADMIN: Full platform access, approve courses, verify users
 * - SYSTEM_ADMIN: Technical admin, database access, system configuration
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
public enum UserRole {
    /**
     * Student role - can enroll in courses and apply for jobs.
     */
    STUDENT,

    /**
     * Instructor role - can create and manage courses.
     */
    INSTRUCTOR,

    /**
     * Employer role - can post jobs and hire candidates.
     */
    EMPLOYER,

    /**
     * Admin role - can moderate content and verify users.
     */
    ADMIN,

    /**
     * System admin role - full technical access.
     */
    SYSTEM_ADMIN
}