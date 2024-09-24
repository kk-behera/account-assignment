package com.dws.challenge.exception;

import com.dws.challenge.domain.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponseDto> handleInsufficientFundsException(InsufficientFundsException e) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(e.getMessage());
        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateAccountIdException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateAccountIdException(DuplicateAccountIdException e) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(e.getMessage());
        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }
}
