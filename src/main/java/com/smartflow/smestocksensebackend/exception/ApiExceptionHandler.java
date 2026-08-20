package com.smartflow.smestocksensebackend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @Autowired
    private MessageSource messageSource;

    private String translate(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }
        try {
            return messageSource.getMessage(message, null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return message;
        }
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleInvalidCredentials(InvalidCredentialsException exception) {
        return new ApiErrorResponse(translate(exception.getMessage()));
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleAuthenticationCredentialsNotFound(
            AuthenticationCredentialsNotFoundException exception) {
        return new ApiErrorResponse(translate(exception.getMessage()));
    }

    @ExceptionHandler({ AccountInactiveException.class, MissingRoleException.class })
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiErrorResponse handleForbidden(RuntimeException exception) {
        return new ApiErrorResponse(translate(exception.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiErrorResponse handleAccessDenied(AccessDeniedException exception) {
        return new ApiErrorResponse(translate(exception.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleBadRequest(BadRequestException exception) {
        return new ApiErrorResponse(translate(exception.getMessage()));
    }

    @ExceptionHandler(FieldValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleFieldValidation(FieldValidationException exception) {
        Map<String, String> translatedErrors = new LinkedHashMap<>();
        if (exception.getErrors() != null) {
            exception.getErrors().forEach((field, msg) -> translatedErrors.put(field, translate(msg)));
        }
        return new ApiErrorResponse(translate(exception.getMessage()), translatedErrors);
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNotFound(NotFoundException exception) {
        return new ApiErrorResponse(translate(exception.getMessage()));
    }

    @ExceptionHandler(UnsupportedMediaTypeException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ApiErrorResponse handleUnsupportedMediaType(UnsupportedMediaTypeException exception) {
        return new ApiErrorResponse(translate(exception.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleConflict(ConflictException exception) {
        return new ApiErrorResponse(translate(exception.getMessage()));
    }

    @ExceptionHandler(CloudinaryConfigurationException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiErrorResponse handleCloudinaryConfiguration(CloudinaryConfigurationException exception) {
        return new ApiErrorResponse(translate(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return new ApiErrorResponse(exception.getName() + " " + translate("không hợp lệ."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), translate(fieldError.getDefaultMessage()));
        }

        return new ApiErrorResponse(translate("Dữ liệu không hợp lệ."), errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleConstraintViolation(ConstraintViolationException exception) {
        return new ApiErrorResponse(translate("Dữ liệu không hợp lệ."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMalformedJson() {
        return new ApiErrorResponse(
                translate("Dữ liệu không hợp lệ."),
                Map.of("body", translate("JSON không hợp lệ hoặc request body bị thiếu.")));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleUnexpected(Exception exception) {
        log.error("Unexpected API error class={} message={}", exception.getClass().getName(), exception.getMessage(),
                exception);
        return new ApiErrorResponse(translate("Lỗi hệ thống. Vui lòng thử lại sau."));
    }
}
