package com.familycareai.common.exception;

public class InvalidTokenException extends AppException {
    public InvalidTokenException(String message) {
        super(ErrorCode.INVALID_TOKEN, message);
    }
}
