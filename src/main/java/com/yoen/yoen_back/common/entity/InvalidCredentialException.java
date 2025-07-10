package com.yoen.yoen_back.common.entity;

import org.springframework.http.HttpStatus;

public class InvalidCredentialException extends ApiException {
    public InvalidCredentialException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
