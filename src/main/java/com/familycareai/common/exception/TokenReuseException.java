package com.familycareai.common.exception;

public class TokenReuseException extends AppException {
    public TokenReuseException(String message) {
        super(ErrorCode.TOKEN_REUSE_DETECTED, message);
    }
}
