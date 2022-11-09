package ca.waaw.web.rest;

import ca.waaw.config.applicationconfig.AppRegexConfig;
import ca.waaw.dto.userdtos.*;
import ca.waaw.web.rest.errors.exceptions.BadRequestException;
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
import java.util.Map;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/api")
@AllArgsConstructor
@Tag(name = "${api.swagger.groups.user}")
public class UserController {

    private final UserService userService;

    private final AppRegexConfig appRegexConfig;

    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAlreadyExist
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.user.checkUsername}")
    @GetMapping("${api.endpoints.user.checkUsername}")
    public void checkUserNameExistence(@RequestParam String username) {
        if (!Pattern.matches(appRegexConfig.getUsername(), username))
            throw new BadRequestException("Please enter a valid username", "username");
        if (userService.checkIfUsernameExists(username))
            throw new EntityAlreadyExistsException("username", username);
    }

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerAlreadyExist
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.user.registerUser}")
    @PostMapping("${api.endpoints.user.registerUser}")
    public void registerNewUser(@Valid @RequestBody NewRegistrationDto registrationDto) {
        userService.registerNewUser(registrationDto);
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.user.verifyEmail}")
    @GetMapping(value = "${api.endpoints.user.verifyEmail}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = UserDetailsNewDto.class))})
    public ResponseEntity<UserDetailsNewDto> verifyEmail(@RequestParam String key) {
        return ResponseEntity.ok(userService.verifyEmail(key));
    }

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerAlreadyExist
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.user.completeRegistration}")
    @PutMapping("${api.endpoints.user.completeRegistration}")
    public void completeRegistration(@Valid @RequestBody CompleteRegistrationDto registrationDto) {
        userService.completeRegistration(registrationDto);
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.user.validatePromoCode}")
    @GetMapping("${api.endpoints.user.validatePromoCode}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(example = "{codeType: STRING, codeValue: INTEGER}"))})
    public ResponseEntity<Map<String, Object>> validatePromoCode(@RequestParam String promoCode) {
        return ResponseEntity.ok(userService.validatePromoCode(promoCode));
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.user.checkInviteKey}")
    @GetMapping("${api.endpoints.user.checkInviteKey}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = UserDetailsNewDto.class))})
    public ResponseEntity<UserDetailsNewDto> checkInviteLink(@RequestParam String key) {
        return ResponseEntity.ok(userService.checkInviteLink(key));
    }

    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.user.acceptInvite}")
    @PutMapping("${api.endpoints.user.acceptInvite}")
    public void acceptInvite(@Valid @RequestBody AcceptInviteDto acceptInviteDto) {
        userService.acceptInvite(acceptInviteDto);
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

}