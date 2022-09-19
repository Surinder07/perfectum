package ca.waaw.web.rest;

import ca.waaw.dto.userdtos.*;
import ca.waaw.web.rest.errors.exceptions.EntityAlreadyExistsException;
import ca.waaw.web.rest.service.UserService;
import ca.waaw.web.rest.utils.customannotations.swagger.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/api")
@AllArgsConstructor
@Tag(name = "${api.swagger.groups.user}")
public class UserController {

    private final UserService userService;

    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAlreadyExist
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.user.checkUsername}")
    @GetMapping("${api.endpoints.user.checkUsername}")
    public void checkUserNameExistence(@RequestParam String username) {
        if (userService.checkIfUsernameExists(username)) throw new EntityAlreadyExistsException("username", username);
    }

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerAlreadyExist
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.user.registerUser}")
    @PostMapping("${api.endpoints.user.registerUser}")
    public void registerNewUser(@Valid @RequestBody RegisterUserDto registerUserDto) {
        userService.registerUser(registerUserDto);
    }

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerAlreadyExist
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.user.registerOrganization}")
    @PostMapping("${api.endpoints.user.registerOrganization}")
    public void registerNewAdminAndOrganization(@Valid @RequestBody RegisterOrganizationDto registerOrganizationDto) {
        userService.registerAdminAndOrganization(registerOrganizationDto);
    }

    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.user.updateUser}")
    @PutMapping("${api.endpoints.user.updateUser}")
    public void updateUser(@Valid @RequestBody UpdateUserDto updateUserDto) {
        userService.updateUserDetails(updateUserDto);
    }

    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.user.updatePassword}")
    @PutMapping("${api.endpoints.user.updatePassword}")
    public void updatePassword(@Valid @RequestBody PasswordUpdateDto passwordUpdateDto) {
        userService.updatePasswordOfLoggedInUser(passwordUpdateDto);
    }

    @SwaggerCreated
    @SwaggerNotFound
    @SwaggerBadRequest
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.user.resetPasswordInit}")
    @GetMapping("${api.endpoints.user.resetPasswordInit}")
    public void initResetPassword(@RequestParam String email) {
        userService.requestPasswordReset(email);
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.user.resetPasswordFinish}")
    @PutMapping("${api.endpoints.user.resetPasswordFinish}")
    public void finishResetPassword(@Valid @RequestBody PasswordResetDto passwordResetDto) {
        userService.completePasswordReset(passwordResetDto);
    }

    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.user.updateProfileImage}")
    @PostMapping("${api.endpoints.user.updateProfileImage}")
    public void updateProfileImage() {
        // TODO Add logic
    }

    @SwaggerAuthenticated
    @Operation(description = "${api.description.user.getUserDetails}")
    @GetMapping("${api.endpoints.user.getUserDetails}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = UserDetailsDto.class))})
    public ResponseEntity<UserDetailsDto> getLoggedInUser() {
        return ResponseEntity.ok(userService.getLoggedInUserAccount());
    }

    /*
     Below are APIs for links sent to email for various purpose. These are not exposed on swagger
     */

    @Operation(hidden = true)
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "${api.endpoints.user.activateAccount}", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> activateAccount(@RequestParam String key) {
        return userService.activateUser(key);
    }

    @Operation(hidden = true)
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("${api.endpoints.user.acceptInvitation}")
    public ResponseEntity<String> acceptInvitation(@RequestParam String key) {
        return userService.acceptInvite(key);
    }

}
