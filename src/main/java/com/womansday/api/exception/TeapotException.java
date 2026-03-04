package com.womansday.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение для статуса 418 I'm a teapot.
 * Используется как пасхалка или для специфической логики.
 */
@ResponseStatus(HttpStatus.I_AM_A_TEAPOT)
public class TeapotException extends RuntimeException {
    public TeapotException(String message) {
        super(message);
    }
}
