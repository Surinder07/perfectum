package ca.waaw.web.rest.errors;

import ca.waaw.enumration.ErrorCodes;
import ca.waaw.enumration.user.Authority;
import ca.waaw.web.rest.errors.exceptions.*;
import ca.waaw.web.rest.errors.exceptions.application.*;
import ca.waaw.web.rest.utils.EmptyStringEditor;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unused")
@ControllerAdvice
@AllArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private final MessageSource messageSource;

    /**
     * This method is present so all our empty string request params will be converted to null values
     */
    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        EmptyStringEditor emptyStringEditor = new EmptyStringEditor();
        dataBinder.registerCustomEditor(String.class, emptyStringEditor);
    }

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
                errors.put(StringUtils.isNotEmpty(fieldName) ? fieldName : "Error", message);
            }
        });
        String[] fields = null;
        try {
            fields = ex.getBindingResult().getAllErrors().stream()
                    .map(e -> ((FieldError) e).getField()).toArray(String[]::new);
        } catch (Exception ignore) {
        }
        return new ResponseEntity<>(new ErrorVM(errors.toString(), fields), HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
                                                             HttpStatus status, WebRequest request) {
        String message = messageSource.getMessage(ErrorMessageKeys.internalServerMessage, null, Locale.ENGLISH);
        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
            return new ResponseEntity<>(new ErrorVM(message), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(body, headers, status);
    }

    @ExceptionHandler(BadRequestException.class)
    protected ResponseEntity<ErrorVM> handleBadRequestException(BadRequestException ex) {
        return new ResponseEntity<>(new ErrorVM(ex.getMessage(), ex.getFields()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    protected ResponseEntity<ErrorVM> handleEmailNotVerifiedException(EmailNotVerifiedException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.emailNotVerifiedMessage, new String[]{ex.getLoginName()},
                Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message, "username/email"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    protected ResponseEntity<ErrorVM> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.usernameNotFoundMessage, new String[]{ex.getLoginName()},
                Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message, "username/email"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    protected ResponseEntity<ErrorVM> handleEntityNotFoundException(EntityNotFoundException ex) {
        String message = ex.getValue() == null ? messageSource.getMessage(ErrorMessageKeys.entityNotFoundMessage,
                new String[]{ex.getEntity()}, Locale.ENGLISH) : messageSource.getMessage(
                ErrorMessageKeys.entityNotFoundWithValueMessage, new String[]{ex.getEntity(), ex.getValue()}, Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message, ex.getEntity()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserAccountDisabledException.class)
    protected ResponseEntity<ErrorVM> handleUserAccountDisabledException(UserAccountDisabledException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.disabledAccountMessage, null, Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message, "username/email"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UnauthorizedException.class)
    protected ResponseEntity<ErrorVM> handleUnauthorizedException(UnauthorizedException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.unauthorizedMessage, null, Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message, "authority"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    protected ResponseEntity<ErrorVM> handleAuthenticationException(AuthenticationException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.authenticationFailedMessage, null, Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message, "login/password"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    protected ResponseEntity<ErrorVM> handleEntityAlreadyExistsException(EntityAlreadyExistsException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.entityAlreadyExistsMessage, new String[]{ex.getEntityName(),
                ex.getEntityType(), ex.getValue()}, Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message, ex.getEntityName()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ExpiredKeyException.class)
    protected ResponseEntity<ErrorVM> handleExpiredKeyException(ExpiredKeyException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.expiredKeyMessage, new String[]{ex.getKeyType()},
                Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message, "key"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PaymentPendingException.class)
    protected ResponseEntity<ErrorVM> handlePaymentPendingException(PaymentPendingException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.paymentPendingEmployeeMessage, null, Locale.ENGLISH);
        if (ex.getRole().equals(Authority.ADMIN))
            return new ResponseEntity<>(new ErrorVM(ErrorCodes.WE_003), HttpStatus.FORBIDDEN);
        else
            return new ResponseEntity<>(new ErrorVM(message), HttpStatus.PAYMENT_REQUIRED);
    }

    @ExceptionHandler(PastValueNotDeletableException.class)
    protected ResponseEntity<ErrorVM> handlePastValueNotDeletableException(PastValueNotDeletableException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.pastValueNotDeletableMessage, new String[]{ex.getEntityType()},
                Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ShiftOverlappingException.class)
    protected ResponseEntity<ErrorVM> handleShiftOverlappingException(ShiftOverlappingException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.shiftOverlappingMessage, null, Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TimesheetOverlappingException.class)
    protected ResponseEntity<ErrorVM> handleTimesheetOverlappingException(TimesheetOverlappingException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.timesheetOverlappingMessage, null, Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MissingHeadersException.class)
    protected ResponseEntity<ErrorVM> handleMissingHeadersException(MissingHeadersException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.missingHeadersMessage, new String[]{ex.getFileType()},
                Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message, ex.getHeaders()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingRequiredFieldsException.class)
    protected ResponseEntity<ErrorVM> handleMissingRequiredFieldsException(MissingRequiredFieldsException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.missingRequiredFieldsMessage, new String[]{ex.getFileType()},
                Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message, ex.getField()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnsupportedFileFormatException.class)
    protected ResponseEntity<ErrorVM> handleUnsupportedFileFormatException(UnsupportedFileFormatException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.unsupportedFileFormatMessage,
                new String[]{Arrays.toString(ex.getAllowedFormats())}, Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(FutureCalenderNotAccessibleException.class)
    protected ResponseEntity<ErrorVM> handleFutureCalenderNotAccessibleException(FutureCalenderNotAccessibleException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.futureCalenderNotAccessibleMessage, null, Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(FileNotReadableException.class)
    protected ResponseEntity<ErrorVM> handleFileNotReadableException(FileNotReadableException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.fileNotReadableMessage, null, Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ForDevelopmentOnlyException.class)
    protected ResponseEntity<ErrorVM> handleForDevelopmentOnlyException(ForDevelopmentOnlyException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.forDevelopmentOnlyMessage, null, Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ActiveTimesheetPresentException.class)
    protected ResponseEntity<ErrorVM> handleActiveTimesheetPresentException(ActiveTimesheetPresentException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.activeTimesheetPresentMessage, null, Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ErrorVM> handleAccessDeniedException(AccessDeniedException ex) {
        return new ResponseEntity<>(new ErrorVM(ex.getErrorCode()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(EntityNotDeletableException.class)
    protected ResponseEntity<ErrorVM> handleEntityNotDeletableException(EntityNotDeletableException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.entityNotDeletableMessage, new String[]{ex.getReasonEntities(),
                        ex.getEntity()}, Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AlreadySubscribedException.class)
    protected ResponseEntity<ErrorVM> handleAlreadySubscribedException(AlreadySubscribedException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.alreadySubscribedMessage, null, Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EntityCantBeActivatedException.class)
    protected ResponseEntity<ErrorVM> handleEntityCantBeActivatedException(EntityCantBeActivatedException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.entityCantBeActivatedMessage, new String[]{ex.getReasonEntities(),
                ex.getEntity()}, Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ShiftNotAssignedException.class)
    protected ResponseEntity<ErrorVM> handleShiftNotAssignedException(ShiftNotAssignedException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.shiftNotAssignedMessage, new String[]{ex.getAction()},
                Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(FileParsingException.class)
    protected ResponseEntity<ErrorVM> handleFileParsingException(FileParsingException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.fileParsingMessage, null, Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(StripeCustomerException.class)
    protected ResponseEntity<ErrorVM> handleStripeCustomerException(StripeCustomerException ex) {
        String message = messageSource.getMessage(ErrorMessageKeys.stripeCustomerMessage, null, Locale.ENGLISH);
        return new ResponseEntity<>(new ErrorVM(message), HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
