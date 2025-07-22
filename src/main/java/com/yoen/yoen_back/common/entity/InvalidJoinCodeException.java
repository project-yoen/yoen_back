package com.yoen.yoen_back.common.entity;

import org.springframework.http.HttpStatus;

public class InvalidJoinCodeException extends ApiException {
    public InvalidJoinCodeException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
