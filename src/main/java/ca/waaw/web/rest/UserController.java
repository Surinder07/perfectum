package ca.waaw.web.rest;

import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.userdtos.*;
import ca.waaw.web.rest.errors.exceptions.EntityAlreadyExistsException;
import ca.waaw.web.rest.service.UserService;
import ca.waaw.web.rest.utils.APIConstants;
import ca.waaw.web.rest.utils.customannotations.ValidateRegex;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.RegexValidatorType;
import ca.waaw.web.rest.utils.customannotations.swagger.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Locale;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/api")
@AllArgsConstructor
@Tag(name = APIConstants.TagNames.user, description = APIConstants.TagDescription.user)
public class UserController {

    private final UserService userService;

    @Operation(description = APIConstants.ApiDescription.User.checkUsername)
    @SwaggerOk
    @SwaggerBadRequest
    @GetMapping(APIConstants.ApiEndpoints.User.checkUsername)
    @ResponseStatus(HttpStatus.OK)
    public void checkUserNameExistence(@RequestParam String username) {
        if (userService.checkIfUsernameExists(username)) throw new EntityAlreadyExistsException("username", username);
    }

    @Operation(description = APIConstants.ApiDescription.User.registerUser)
    @SwaggerCreated
    @SwaggerBadRequest
    @PostMapping(APIConstants.ApiEndpoints.User.registerUser)
    @ResponseStatus(HttpStatus.CREATED)
    public void registerNewUser(@Valid @RequestBody RegisterUserDto registerUserDto) {
        userService.registerUser(registerUserDto);
    }

    @Operation(description = APIConstants.ApiDescription.User.registerOrganization)
    @SwaggerCreated
    @SwaggerBadRequest
    @PostMapping(APIConstants.ApiEndpoints.User.registerOrganization)
    @ResponseStatus(HttpStatus.CREATED)
    public void registerNewAdminAndOrganization(@Valid @RequestBody RegisterOrganizationDto registerOrganizationDto) {
        userService.registerAdminAndOrganization(registerOrganizationDto);
    }

    @Operation(description = APIConstants.ApiDescription.User.updateUser)
    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @PutMapping(APIConstants.ApiEndpoints.User.updateUser)
    @ResponseStatus(HttpStatus.OK)
    public void updateUser(@Valid @RequestBody UpdateUserDto updateUserDto) {
        userService.updateUserDetails(updateUserDto);
    }

    @Operation(description = APIConstants.ApiDescription.User.updatePassword)
    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @PutMapping(APIConstants.ApiEndpoints.User.updatePassword)
    @ResponseStatus(HttpStatus.OK)
    public void updatePassword(@Valid @RequestBody PasswordUpdateDto passwordUpdateDto) {
        userService.updatePasswordOfLoggedInUser(passwordUpdateDto);
    }

    @Operation(description = APIConstants.ApiDescription.User.resetPasswordInit)
    @SwaggerCreated
    @SwaggerBadRequest
    @GetMapping(APIConstants.ApiEndpoints.User.resetPasswordInit)
    @ResponseStatus(HttpStatus.CREATED)
    public void initResetPassword(@Valid @RequestParam @ValidateRegex(type = RegexValidatorType.EMAIL) String email) {
        userService.requestPasswordReset(email);
    }

    @Operation(description = APIConstants.ApiDescription.User.resetPasswordFinish)
    @SwaggerOk
    @SwaggerBadRequest
    @PutMapping(APIConstants.ApiEndpoints.User.resetPasswordFinish)
    @ResponseStatus(HttpStatus.OK)
    public void finishResetPassword(@Valid @RequestBody PasswordResetDto passwordResetDto) {
        userService.completePasswordReset(passwordResetDto);
    }

    @Operation(description = APIConstants.ApiDescription.User.updateProfileImage)
    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @PostMapping(APIConstants.ApiEndpoints.User.updateProfileImage)
    @ResponseStatus(HttpStatus.OK)
    public void updateProfileImage() {
        // TODO Add logic
    }

    @Operation(hidden = true)
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = APIConstants.ApiEndpoints.User.activateAccount, produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> activateAccount(@RequestParam String key) {
        return userService.activateUser(key);
    }

    @Operation(hidden = true)
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = APIConstants.ApiEndpoints.User.acceptInvitation)
    public ResponseEntity<String> acceptInvitation(@RequestParam String key) {
        return userService.acceptInvite(key);
    }

    @Operation(description = APIConstants.ApiDescription.User.sendInvite)
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @PostMapping(value = APIConstants.ApiEndpoints.User.sendInvite)
    @ResponseStatus(HttpStatus.CREATED)
    public void sendInvite(@Valid @RequestBody InviteUserDto inviteUserDto) {
        userService.inviteNewUsers(inviteUserDto);
    }

    @Operation(description = APIConstants.ApiDescription.User.getUserDetails)
    @SwaggerAuthenticated
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = UserDetailsDto.class))})
    @GetMapping(APIConstants.ApiEndpoints.User.getUserDetails)
    public ResponseEntity<UserDetailsDto> getLoggedInUser() {
        return ResponseEntity.ok(userService.getLoggedInUserAccount());
    }

    @SwaggerBadRequest
    @Operation(description = APIConstants.ApiDescription.User.getAllUsers)
    @SwaggerAuthenticated
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = UserDetailsForAdminDto.class)))},
            description = "${api.swagger.schema-description.pagination}")
    @GetMapping(APIConstants.ApiEndpoints.User.getAllUsers)
    public ResponseEntity<PaginationDto> getAllUsers(@PathVariable int pageNo, @PathVariable int pageSize,
                                                     @Parameter(description = "${api.swagger.param-description.getUsersSearchKey}")
                                                     @RequestParam(required = false) String searchKey,
                                                     @Parameter(description = "${api.swagger.param-description.getUsersLocation}")
                                                     @RequestParam(required = false) String locationId,
                                                     @Parameter(description = "${api.swagger.param-description.getUsersRole}")
                                                     @RequestParam(required = false) String role) {
        role = StringUtils.isNotEmpty(role) ? role.toUpperCase(Locale.ROOT) : null;
        locationId = StringUtils.isNotEmpty(locationId) ? locationId : null;
        return ResponseEntity.ok(userService.getAllUsers(pageNo, pageSize, searchKey, locationId, role));
    }

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.user.listAllUsers}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = UserInfoForDropDown.class)))},
            description = "${api.swagger.schema-description.pagination}")
    @GetMapping("${api.endpoints.user.listAllUsers}")
    public ResponseEntity<PaginationDto> listAllUsers(@PathVariable int pageNo, @PathVariable int pageSize,
                                                     @Parameter(description = "${api.swagger.param-description.getUsersSearchKey}")
                                                     @RequestParam(required = false) String searchKey,
                                                     @Parameter(description = "${api.swagger.param-description.getUsersLocationRole}")
                                                     @RequestParam String locationRoleId) {
        return ResponseEntity.ok(userService.listAllUsers(pageNo, pageSize, searchKey, locationRoleId));
    }

    @Operation(summary = "Update organization preferences under logged-in admin")
    @SwaggerAuthenticated
    @SwaggerUnauthorized
    @SwaggerOk
    @PutMapping("/v1/organization/update")
    public void updateOrganizationPreferences(@RequestBody OrganizationPreferences preferences) {
        userService.updateOrganizationPreferences(preferences);
    }

}
