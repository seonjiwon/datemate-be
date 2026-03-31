package com.datemate.global.exception.handler;


import com.datemate.global.CustomResponse;
import com.datemate.global.code.error.BaseErrorCode;
import com.datemate.global.code.error.GeneralErrorCode;
import com.datemate.global.exception.CustomException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // CustomException 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<CustomResponse<?>> handleCustomException(CustomException ex) {
        log.warn("[ CustomException ]: {}", ex.getCode().getMessage());

        BaseErrorCode errorCode = ex.getCode();

        // errorCode가 반환할 응답을 직접 생성하도록 변경
        CustomResponse<?> errorResponse = CustomResponse.onFailure(errorCode);

        return ResponseEntity
            .status(errorCode.getStatus())
            .body(errorResponse);
    }

    // 컨트롤러 메서드에서 @Valid 어노테이션을 사용하여 DTO의 유효성 검사를 수행
    // @NotBlank, @Email 등이 붙어 있는데, 값이 잘못되면 이 예외 발생
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<CustomResponse<Map<String, String>>> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );

        BaseErrorCode validationErrorCode = GeneralErrorCode.VALIDATION_FAILED;

        CustomResponse<Map<String, String>> errorResponse = CustomResponse.onFailure(
            validationErrorCode.getStatus(),
            validationErrorCode.getCode(),
            validationErrorCode.getMessage(),
            false,
            errors);

        return ResponseEntity.status(validationErrorCode.getStatus()).body(errorResponse);
    }

    // 예상치 못한 일반 예외 처리
    @ExceptionHandler({Exception.class})
    public ResponseEntity<CustomResponse<String>> handleGeneralException(Exception ex) {
        log.error("Internal Server Error", ex);

        BaseErrorCode errorCode = GeneralErrorCode.INTERNAL_SERVER_ERROR_500;

        // 코드/메시지 기반 실패 응답 생성 방식 통일
        CustomResponse<String> errorResponse = CustomResponse.onFailure(
            errorCode.getStatus(),
            errorCode.getCode(),
            errorCode.getMessage(),
            false,
            null
        );

        return ResponseEntity
            .status(errorCode.getStatus())
            .body(errorResponse);
    }
}
