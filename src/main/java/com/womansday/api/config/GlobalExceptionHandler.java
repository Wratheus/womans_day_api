package com.womansday.api.config;

import com.womansday.api.exception.BusinessLogicException;
import com.womansday.api.exception.ResourceNotFoundException;
import com.womansday.api.exception.TeapotException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TeapotException.class)
    public ResponseEntity<Map<String, Object>> handleTeapot(TeapotException e) {
        return buildResponse(HttpStatus.I_AM_A_TEAPOT, e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessLogic(BusinessLogicException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        err -> err.getField(),
                        err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : "Ошибка валидации",
                        (a, b) -> a));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", "Ошибка валидации");
        body.put("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
        return buildResponse(HttpStatus.CONFLICT, "Ресурс был изменён другим запросом, пожалуйста, повторите попытку");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        long maxBytes = e.getMaxUploadSize();
        String limitMessage = maxBytes > 0
                ? String.format("Максимальный размер: %d МБ", maxBytes / (1024 * 1024))
                : "Максимальный размер: 200 МБ";
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "Файл слишком большой. " + limitMessage);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleBadJson(HttpMessageNotReadableException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Некорректное тело запроса");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Отсутствует обязательный параметр: " + e.getParameterName());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingPart(MissingServletRequestPartException e) {
        String hint = "avatar".equals(e.getRequestPartName())
                ? " Используйте form-data с ключом 'avatar' и типом File."
                : " Используйте form-data с ключом '" + e.getRequestPartName() + "'.";
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Отсутствует обязательная часть запроса: " + e.getRequestPartName() + "." + hint);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipart(MultipartException e) {
        log.warn("Multipart error: {}", e.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Некорректный multipart-запрос. Не задавайте Content-Type вручную — используйте Body → form-data и добавьте файл с нужным ключом (avatar / photos).");
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurity(SecurityException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception e) {
        log.error("Unhandled exception", e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
