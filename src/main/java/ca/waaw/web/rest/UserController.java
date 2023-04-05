package ca.waaw.web.rest;

import ca.waaw.config.applicationconfig.AppRegexConfig;
import ca.waaw.dto.userdtos.*;
import ca.waaw.web.rest.service.UserService;
import ca.waaw.web.rest.utils.customannotations.swagger.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.Map;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/api")
@AllArgsConstructor
@Tag(name = "${api.swagger.groups.user}")
public class UserController {

    private final Logger log = LogManager.getLogger(UserController.class);

    private final UserService userService;

    private final AppRegexConfig appRegexConfig;

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerAlreadyExist
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.user.registerUser}")
    @PostMapping("${api.endpoints.user.registerUser}")
    public void registerNewUser(@Valid @RequestBody NewRegistrationDto registrationDto) {
        try {
            userService.registerNewUser(registrationDto);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.user.verifyEmail}")
    @GetMapping(value = "${api.endpoints.user.verifyEmail}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json")})
    public void verifyEmail(@RequestParam String key) {
        userService.verifyEmail(key);
    }

    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAlreadyExist
    @ResponseStatus(HttpStatus.OK)
    @ApiResponse(responseCode = "200", description = "Success", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = LoginResponseDto.class, description = "Updates the username in jwt"))})
    @Operation(description = "${api.description.user.completeRegistration}")
    @PutMapping("${api.endpoints.user.completeRegistration}")
    public ResponseEntity<LoginResponseDto> completeRegistration(@Valid @RequestBody CompleteRegistrationDto registrationDto) {
        try {
            return ResponseEntity.ok(userService.completeRegistration(registrationDto));
        } catch (Exception e) {
            log.error("Exception occurred while completing user profile", e);
            throw e;
        }
    }

    @SwaggerOk
    @SwaggerBadRequest
    @ResponseStatus(HttpStatus.OK)
    @ApiResponse(responseCode = "200", description = "Success", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = LoginResponseDto.class, description = "Updates the username in jwt"))})
    @Operation(description = "${api.description.user.completePaymentInfo}")
    @GetMapping("${api.endpoints.user.completePaymentInfo}")
    public ResponseEntity<LoginResponseDto> completePaymentInfo(@RequestParam String stripeTokenId) {
        try {
            return ResponseEntity.ok(userService.completePaymentInfo(stripeTokenId));
        } catch (Exception e) {
            log.error("Exception occurred while completing user profile", e);
            throw e;
        }
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.user.validatePromoCode}")
    @GetMapping("${api.endpoints.user.validatePromoCode}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(example = "{message: Trial period for 10 days applied.}"))})
    public ResponseEntity<Map<String, String>> validatePromoCode(@RequestParam String promoCode) {
        return ResponseEntity.ok(userService.validatePromoCode(promoCode));
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.user.checkInviteKey}")
    @GetMapping("${api.endpoints.user.checkInviteKey}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = UserListingDto.class))})
    public ResponseEntity<UserListingDto> checkInviteLink(@RequestParam String key) {
        return ResponseEntity.ok(userService.checkInviteLink(key));
    }

    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.user.acceptInvite}")
    @PutMapping("${api.endpoints.user.acceptInvite}")
    public void acceptInvite(@Valid @RequestBody AcceptInviteDto acceptInviteDto) {
        try {
            userService.acceptInvite(acceptInviteDto);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
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
    public void updateProfileImage(@RequestPart MultipartFile file) throws Exception {
        userService.updateProfilePic(file);
    }

    @SwaggerAuthenticated
    @Operation(description = "${api.description.user.getUserDetails}")
    @GetMapping("${api.endpoints.user.getUserDetails}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = UserDetailsDto.class))})
    public ResponseEntity<UserDetailsDto> getLoggedInUser() {
        return ResponseEntity.ok(userService.getLoggedInUserAccount());
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.user.updateUser}")
    @PutMapping("${api.endpoints.user.updateUser}")
    public void updateUserDetails(@Valid @RequestBody UpdateLoggedUserDto updateLoggedUserDto) {
        userService.updateLoggedUserDetails(updateLoggedUserDto);
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.user.updateEmailInit}")
    @GetMapping("${api.endpoints.user.updateEmailInit}")
    public void updateUserEmail(@RequestParam String email) {
        try {
            userService.updateEmailInit(email);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerAuthenticated
    @Operation(description = "${api.description.user.updateEmailFinish}")
    @GetMapping("${api.endpoints.user.updateEmailFinish}")
    public void verifyUpdatedEmail(@RequestParam String token) {
        userService.updateEmailFinish(token);
    }

    // TODO Add API to toggle mobile and email notification preference

}