package com.wrap.global.exception;

import com.wrap.global.common.ApiResponse;
import com.wrap.global.common.ApiResponse.ErrorBody;
import com.wrap.global.common.ApiResponse.FieldError;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<?>> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        ErrorBody errorBody = new ErrorBody(errorCode.getCode(), errorCode.getMessage(), List.of());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.fail(errorBody));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        List<FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ErrorBody errorBody = new ErrorBody(
                ErrorCode.VALIDATION_FAILED.getCode(),
                ErrorCode.VALIDATION_FAILED.getMessage(),
                fieldErrors
        );
        return ResponseEntity.badRequest().body(ApiResponse.fail(errorBody));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        ErrorBody errorBody = new ErrorBody(errorCode.getCode(), errorCode.getMessage(), List.of());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.fail(errorBody));
    }
}
