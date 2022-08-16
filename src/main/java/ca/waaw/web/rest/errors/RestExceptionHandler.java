package ca.waaw.web.rest.errors;

import ca.waaw.web.rest.errors.exceptions.*;
import ca.waaw.web.rest.utils.CommonUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatus status, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = "";
            try {
                fieldName = ((FieldError) error).getField();
            } catch (Exception ignore) {
            }
            String message = error.getDefaultMessage();
            if (errors.containsKey(fieldName)) {
                errors.put(fieldName, errors.get(fieldName) + "; " + message);
            } else {
                errors.put(fieldName, message);
            }
        });
        String[] fields = null;
        try {
            fields = ex.getBindingResult().getAllErrors().stream()
                    .map(e -> ((FieldError) e).getField()).toArray(String[]::new);
        } catch (Exception ignore) {}
        return new ResponseEntity<>(new ErrorVM(errors.toString(), fields), HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
                                                             HttpStatus status, WebRequest request) {
        String message = CommonUtils.getPropertyFromMessagesResourceBundle(ErrorMessageKeys.internalServerMessage, null);
        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
            return new ResponseEntity<>(new ErrorVM(message, ""), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(body, headers, status);
    }

    @ExceptionHandler(BadRequestException.class)
    protected ResponseEntity<ErrorVM> handleBadRequestException(BadRequestException ex) {
        return new ResponseEntity<>(new ErrorVM(ex.getMessage(), ex.getFields()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserNotActivatedException.class)
    protected ResponseEntity<ErrorVM> handleUserNotActivatedException(UserNotActivatedException ex) {
        String message = String.format(CommonUtils.getPropertyFromMessagesResourceBundle(ErrorMessageKeys.userNotActivatedMessage,
                null), ex.getMessage());
        return new ResponseEntity<>(new ErrorVM(message, "username/email"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    protected ResponseEntity<ErrorVM> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        String message = String.format(CommonUtils.getPropertyFromMessagesResourceBundle(ErrorMessageKeys.usernameNotFoundMessage,
                null), ex.getMessage());
        return new ResponseEntity<>(new ErrorVM(message, "username/email"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserAccountDisabledException.class)
    protected ResponseEntity<ErrorVM> handleUserAccountDisabledException(UserAccountDisabledException ex) {
        String message = CommonUtils.getPropertyFromMessagesResourceBundle(ErrorMessageKeys.disabledAccountMessage, null);
        return new ResponseEntity<>(new ErrorVM(message, "username/email"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UnauthorizedException.class)
    protected ResponseEntity<ErrorVM> handleUnauthorizedException(UnauthorizedException ex) {
        String message = CommonUtils.getPropertyFromMessagesResourceBundle(ErrorMessageKeys.unauthorizedMessage, null);
        return new ResponseEntity<>(new ErrorVM(message, "authority"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    protected ResponseEntity<ErrorVM> handleAuthenticationException(AuthenticationException ex) {
        String message = CommonUtils.getPropertyFromMessagesResourceBundle(ErrorMessageKeys.authenticationFailedMessage, null);
        return new ResponseEntity<>(new ErrorVM(message, "login/password"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    protected ResponseEntity<ErrorVM> handleEntityAlreadyExistsException(EntityAlreadyExistsException ex) {
        String message = String.format(CommonUtils.getPropertyFromMessagesResourceBundle(ErrorMessageKeys.entityAlreadyExistsMessage,
                null), ex.getEntityName(), ex.getValue());
        return new ResponseEntity<>(new ErrorVM(message, ex.getEntityName()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ExpiredKeyException.class)
    protected ResponseEntity<ErrorVM> handleExpiredKeyException(ExpiredKeyException ex) {
        String message = String.format(CommonUtils.getPropertyFromMessagesResourceBundle(ErrorMessageKeys.expiredKeyMessage,
                null), ex.getMessage());
        return new ResponseEntity<>(new ErrorVM(message, "key"), HttpStatus.NOT_FOUND);
    }

}
