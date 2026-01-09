package com.mdaudev.mwanzo.mwanzocourseplatformbackend.exception;

import java.util.UUID;

/**
 * Resource Not Found Exception
 *
 * Thrown when a requested resource (course, category, user, etc.)
 * does not exist in the database.
 *
 * Results in HTTP 404 response.
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    /**
     * Constructor with resource details.
     *
     * @param resourceName Name of resource (e.g., "Course", "Category")
     * @param fieldName Field used to search (e.g., "id", "slug")
     * @param fieldValue Value that was searched for
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    /**
     * Simplified constructor.
     *
     * @param message Error message
     */
    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceName = "Resource";
        this.fieldName = "identifier";
        this.fieldValue = "unknown";
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
}