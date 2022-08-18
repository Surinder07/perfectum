package ca.waaw.web.rest;

import ca.waaw.dto.userdtos.*;
import ca.waaw.web.rest.service.UserService;
import ca.waaw.web.rest.errors.exceptions.EntityAlreadyExistsException;
import ca.waaw.web.rest.utils.customannotations.ValidateRegex;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.RegexValidatorType;
import ca.waaw.web.rest.utils.customannotations.swagger.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/api")
@AllArgsConstructor
@Tag(name = "User Account", description = "All User account related APIs")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Will Return Success only if given username is available")
    @SwaggerOk
    @SwaggerBadRequest
    @GetMapping("/v1/unAuth/checkUserNameExistence")
    @ResponseStatus(HttpStatus.OK)
    public void checkUserNameExistence(@Valid @RequestParam @ValidateRegex(type = RegexValidatorType.USERNAME) String username) {
        if(userService.checkIfUsernameExists(username)) throw new EntityAlreadyExistsException("username", username);
    }

    @Operation(summary = "Register a new user (by email invite only)")
    @SwaggerCreated
    @SwaggerBadRequest
    @PostMapping("/v1/unAuth/registerUser")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerNewUser(@Valid @RequestBody RegisterUserDto registerUserDto) {
        userService.registerUser(registerUserDto);
    }

    @Operation(summary = "Register a new user (admin) with an organization")
    @SwaggerCreated
    @SwaggerBadRequest
    @PostMapping("/v1/unAuth/registerAdmin")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerNewAdminAndOrganization(@Valid @RequestBody RegisterOrganizationDto registerOrganizationDto) {
        userService.registerAdminAndOrganization(registerOrganizationDto);
    }

    @Operation(summary = "Update logged in user details")
    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @PutMapping("/v1/updateUser")
    @ResponseStatus(HttpStatus.OK)
    public void updateUser(@Valid @RequestBody UpdateUserDto updateUserDto) {
        userService.updateUserDetails(updateUserDto);
    }

    @Operation(summary = "Update current password using the old password")
    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @PutMapping("v1/updatePassword")
    @ResponseStatus(HttpStatus.OK)
    public void updatePassword(@Valid @RequestBody PasswordUpdateDto passwordUpdateDto) {
        userService.updatePasswordOfLoggedInUser(passwordUpdateDto);
    }

    @Operation(summary = "Initialize a password reset request and get email to reset password")
    @SwaggerCreated
    @SwaggerBadRequest
    @GetMapping("/v1/unAuth/resetPassword/init")
    @ResponseStatus(HttpStatus.CREATED)
    public void initResetPassword(@Valid @RequestParam @ValidateRegex(type = RegexValidatorType.EMAIL) String email) {
        userService.requestPasswordReset(email);
    }

    @Operation(summary = "Finish password reset request with key received on email")
    @SwaggerOk
    @SwaggerBadRequest
    @PutMapping("/v1/unAuth/resetPassword/finish")
    @ResponseStatus(HttpStatus.OK)
    public void finishResetPassword(@Valid @RequestBody PasswordResetDto passwordResetDto) {
        userService.completePasswordReset(passwordResetDto);
    }

    @Operation(summary = "Update profile image for logged in user", hidden = true)
    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @PostMapping("/v1/updateProfileImage")
    @ResponseStatus(HttpStatus.OK)
    public void updateProfileImage() {
        // TODO Add logic and remove hidden = true from Operation annotation
    }

    @Operation(hidden = true)
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/v1/unAuth/activateAccount", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> activateAccount(@RequestParam String key) {
        return userService.activateUser(key);
    }

    @Operation(hidden = true)
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/v1/unAuth/acceptInvitation")
    public ResponseEntity<String> acceptInvitation(@RequestParam String key) {
        return userService.acceptInvite(key);
    }

    @Operation(summary = "Invite new users to join logged in admins organization")
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @PostMapping(value = "/v1/sendInvite")
    @ResponseStatus(HttpStatus.CREATED)
    public void sendInvite(@Valid @RequestBody InviteUserDto inviteUserDto) {
        userService.inviteNewUsers(inviteUserDto);
    }

    @Operation(summary = "Get all Logged in user's account details")
    @SwaggerAuthenticated
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = UserDetailsDto.class))})
    @GetMapping("/v1/getAccount")
    public ResponseEntity<UserDetailsDto> getLoggedInUser() {
        return ResponseEntity.ok(userService.getLoggedInUserAccount());
    }

    @Operation(summary = "Get all Employees and Admins under logged-in user")
    @SwaggerAuthenticated
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = UserDetailsForAdminDto.class)))})
    @GetMapping("/v1/users/getAll")
    public ResponseEntity<List<UserDetailsForAdminDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

}
