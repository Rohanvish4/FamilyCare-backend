package com.familycareai.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_REQUEST("ERR_INVALID_REQUEST", "Invalid request parameters", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED("ERR_VALIDATION_FAILED", "Validation failed for input fields", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("ERR_UNAUTHORIZED", "Authentication required or invalid credentials", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("ERR_INVALID_TOKEN", "Invalid or expired token", HttpStatus.UNAUTHORIZED),
    TOKEN_REUSE_DETECTED("ERR_TOKEN_REUSE_DETECTED", "Security violation: Revoked refresh token reuse detected", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("ERR_FORBIDDEN", "Access denied for requested resource", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("ERR_RESOURCE_NOT_FOUND", "Requested resource was not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("ERR_USER_ALREADY_EXISTS", "User already exists with given email or phone number", HttpStatus.CONFLICT),
    ACCOUNT_LOCKED("ERR_ACCOUNT_LOCKED", "Account is temporarily locked due to multiple failed login attempts", HttpStatus.LOCKED),
    INTERNAL_SERVER_ERROR("ERR_INTERNAL_SERVER", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }
}
