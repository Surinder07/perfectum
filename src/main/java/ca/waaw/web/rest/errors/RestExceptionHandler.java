package ca.waaw.web.rest.errors;

import ca.waaw.web.rest.errors.exceptions.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            if (errors.containsKey(fieldName)) {
                errors.put(fieldName, errors.get(fieldName) + "; " + message);
            } else {
                errors.put(fieldName, message);
            }
        });
        String[] fields = ex.getBindingResult().getAllErrors().stream()
                .map(e -> ((FieldError) e).getField()).toArray(String[]::new);
        return new ResponseEntity<>(new ErrorVM(errors.toString(), fields), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InternalServerException.class)
    protected ResponseEntity<ErrorVM> handleInternalServerException(InternalServerException ex) {
        return new ResponseEntity<>(new ErrorVM(ErrorConstants.internalServerMessage, ""), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BadRequestException.class)
    protected ResponseEntity<ErrorVM> handleBadRequestException(BadRequestException ex) {
        return new ResponseEntity<>(new ErrorVM(ex.getMessage(), ex.getFields()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserNotActivatedException.class)
    protected ResponseEntity<ErrorVM> handleUserNotActivatedException(UserNotActivatedException ex) {
        String message = String.format(ErrorConstants.userNotActivatedMessage, ex.getMessage());
        return new ResponseEntity<>(new ErrorVM(message, "username/email"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    protected ResponseEntity<ErrorVM> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        String message = String.format(ErrorConstants.usernameNotFoundMessage, ex.getMessage());
        return new ResponseEntity<>(new ErrorVM(message, "username/email"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserAccountDisabledException.class)
    protected ResponseEntity<ErrorVM> handleUserAccountDisabledException(UserAccountDisabledException ex) {
        return new ResponseEntity<>(new ErrorVM(ErrorConstants.disabledAccountMessage, "username/email"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UnauthorizedException.class)
    protected ResponseEntity<ErrorVM> handleUnauthorizedException(UnauthorizedException ex) {
        return new ResponseEntity<>(new ErrorVM(ErrorConstants.unauthorizedMessage, "authority"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    protected ResponseEntity<ErrorVM> handleAuthenticationException(AuthenticationException ex) {
        return new ResponseEntity<>(new ErrorVM(ErrorConstants.authenticationFailedMessage, "login/password"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    protected ResponseEntity<ErrorVM> handleEntityAlreadyExistsException(EntityAlreadyExistsException ex) {
        String message = String.format(ErrorConstants.entityAlreadyExistsMessage, ex.getEntityName(), ex.getValue());
        return new ResponseEntity<>(new ErrorVM(message, ex.getEntityName()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ExpiredKeyException.class)
    protected ResponseEntity<ErrorVM> handleExpiredKeyException(ExpiredKeyException ex) {
        String message = String.format(ErrorConstants.expiredKeyMessage, ex.getMessage());
        return new ResponseEntity<>(new ErrorVM(message, "key"), HttpStatus.NOT_FOUND);
    }

}
