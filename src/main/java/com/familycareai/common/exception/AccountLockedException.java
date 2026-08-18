package com.familycareai.common.exception;

public class AccountLockedException extends AppException {
    public AccountLockedException(String message) {
        super(ErrorCode.ACCOUNT_LOCKED, message);
    }
}
