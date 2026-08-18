package com.familycareai.common.exception;

public class UserAlreadyExistsException extends AppException {
    public UserAlreadyExistsException(String message) {
        super(ErrorCode.USER_ALREADY_EXISTS, message);
    }
}
