package ca.waaw.web.rest;

import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.userdtos.*;
import ca.waaw.web.rest.errors.exceptions.EntityAlreadyExistsException;
import ca.waaw.web.rest.service.UserService;
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

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.user.sendInvite}")
    @PostMapping("${api.endpoints.user.sendInvite}")
    public void sendInvite(@Valid @RequestBody InviteUserDto inviteUserDto) {
        userService.inviteNewUsers(inviteUserDto);
    }

    @SwaggerAuthenticated
    @Operation(description = "${api.description.user.getUserDetails}")
    @GetMapping("${api.endpoints.user.getUserDetails}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = UserDetailsDto.class))})
    public ResponseEntity<UserDetailsDto> getLoggedInUser() {
        return ResponseEntity.ok(userService.getLoggedInUserAccount());
    }

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.user.getAllUsers}")
    @GetMapping("${api.endpoints.user.getAllUsers}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = UserDetailsForAdminDto.class)))},
            description = "${api.swagger.schema-description.pagination}")
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
    @GetMapping("${api.endpoints.user.listAllUsers}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = UserInfoForDropDown.class)))},
            description = "${api.swagger.schema-description.pagination}")
    public ResponseEntity<PaginationDto> listAllUsers(@PathVariable int pageNo, @PathVariable int pageSize,
                                                     @Parameter(description = "${api.swagger.param-description.getUsersSearchKey}")
                                                     @RequestParam(required = false) String searchKey,
                                                     @RequestParam String locationRoleId) {
        return ResponseEntity.ok(userService.listAllUsers(pageNo, pageSize, searchKey, locationRoleId));
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
