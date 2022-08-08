package ca.waaw.web.rest.errors;

public class ErrorConstants {

    public static final String userNotActivatedMessage = "User with username or email (%s) is not activated yet.";
    public static final String usernameNotFoundMessage = "User with username or email (%s) not found.";
    public static final String expiredKeyMessage = "The %s key you are using seems to be invalid or expired. Please contact Admin.";
    public static final String disabledAccountMessage = "The account you are logging in with has been disabled.";
    public static final String unauthorizedMessage = "You are not authorized to access this resource.";
    public static final String authenticationFailedMessage = "You are not authorized to access this resource.";
    public static final String internalServerMessage = "There was some unexpected error. Please contact admin.";
    public static final String entityAlreadyExistsMessage = "A/an %s with value (%s) already exists.";

}
