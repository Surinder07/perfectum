package ca.waaw.web.rest;

import ca.waaw.config.applicationconfig.AppUrlConfig;
import ca.waaw.dto.*;
import ca.waaw.service.UserService;
import ca.waaw.web.rest.errors.exceptions.EntityAlreadyExistsException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    private final AppUrlConfig appUrlConfig;

    @GetMapping("/v1/unAuth/checkUserNameExistence")
    @ResponseStatus(HttpStatus.OK)
    public void checkUserNameExistence(@RequestParam String username) {
        if(userService.checkIfUsernameExists(username)) throw new EntityAlreadyExistsException("username", username);
    }

    @PostMapping("/v1/unAuth/registerUser")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerNewUser(@Valid @RequestBody RegisterUserDto registerUserDto) {
        userService.registerUser(registerUserDto);
    }

    @PostMapping("/v1/unAuth/registerAdmin")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerNewAdminAndOrganization(@Valid @RequestBody RegisterOrganizationDto registerOrganizationDto) {
        userService.registerAdminAndOrganization(registerOrganizationDto);
    }

    @PutMapping("/v1/updateUser")
    @ResponseStatus(HttpStatus.OK)
    public void updateUser() {

    }

    @PutMapping("v1/updatePassword")
    @ResponseStatus(HttpStatus.CREATED)
    public void updatePassword(@Valid @RequestBody PasswordUpdateDto passwordUpdateDto) {
        userService.updatePasswordOfLoggedInUser(passwordUpdateDto);
    }

    @GetMapping("/v1/unAuth/resetPassword/init")
    @ResponseStatus(HttpStatus.CREATED)
    public void initResetPassword(@RequestParam String email) {
        userService.requestPasswordReset(email);
    }

    @PutMapping("/v1/unAuth/resetPassword/finish")
    @ResponseStatus(HttpStatus.CREATED)
    public void finishResetPassword(@Valid @RequestBody PasswordResetDto passwordResetDto) {
        userService.completePasswordReset(passwordResetDto);
    }

    @PostMapping("/v1/updateProfileImage")
    @ResponseStatus(HttpStatus.CREATED)
    public void updateProfileImage() {

    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/v1/unAuth/activateAccount", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> activateAccount(@RequestParam String key) {
        String response = userService.activateUser(key);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Refresh", String.format("2;url=%s", appUrlConfig.getLoginUrl()));
        return new ResponseEntity<>(response, httpHeaders, HttpStatus.OK);
    }

    @PostMapping(value = "/v1/sendInvite")
    @ResponseStatus(HttpStatus.CREATED)
    public void sendInvite(@Valid @RequestBody InviteUserDto inviteUserDto) {
        userService.inviteNewUsers(inviteUserDto);
    }

    @GetMapping("/v1/getAccount")
    public ResponseEntity<UserDetailsDto> getLoggedInUser() {
        return ResponseEntity.ok(userService.getLoggedInUserAccount());
    }

}
