package com.wrap.global.exception;

import com.wrap.global.common.ApiResponse;
import com.wrap.global.common.ApiResponse.ErrorBody;
import com.wrap.global.common.ApiResponse.FieldError;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        ErrorBody errorBody = new ErrorBody(
                ErrorCode.INVALID_REQUEST.getCode(),
                ErrorCode.INVALID_REQUEST.getMessage(),
                List.of(new FieldError(e.getName(), "요청 파라미터 형식이 올바르지 않습니다."))
        );
        return ResponseEntity.badRequest().body(ApiResponse.fail(errorBody));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Unhandled exception occurred", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        ErrorBody errorBody = new ErrorBody(errorCode.getCode(), errorCode.getMessage(), List.of());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.fail(errorBody));
    }
}
