package ca.waaw.web.rest;

import ca.waaw.dto.EmployeePreferencesDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.userdtos.InviteUserDto;
import ca.waaw.dto.userdtos.UserDetailsForAdminDto;
import ca.waaw.dto.userdtos.UserInfoForDropDown;
import ca.waaw.web.rest.service.MemberService;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerAuthenticated;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerBadRequest;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerCreated;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerUnauthorized;
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
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.member}")
public class MemberController {

    private final MemberService memberService;

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.member.sendInvite}")
    @PostMapping("${api.endpoints.member.sendInvite}")
    public void sendInvite(@Valid @RequestBody InviteUserDto inviteUserDto) {
        memberService.inviteNewUsers(inviteUserDto);
    }

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.member.getAllMembers}")
    @GetMapping("${api.endpoints.member.getAllMembers}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = UserDetailsForAdminDto.class)))},
            description = "${api.swagger.schema-description.pagination}")
    public ResponseEntity<PaginationDto> getAllMembers(@PathVariable int pageNo, @PathVariable int pageSize,
                                                       @Parameter(description = "${api.swagger.param-description.getUsersSearchKey}")
                                                       @RequestParam(required = false) String searchKey,
                                                       @Parameter(description = "${api.swagger.param-description.getUsersLocation}")
                                                       @RequestParam(required = false) String locationId,
                                                       @Parameter(description = "${api.swagger.param-description.getUsersRole}")
                                                       @RequestParam(required = false) String role) {
        role = StringUtils.isNotEmpty(role) ? role.toUpperCase(Locale.ROOT) : null;
        locationId = StringUtils.isNotEmpty(locationId) ? locationId : null;
        return ResponseEntity.ok(memberService.getAllUsers(pageNo, pageSize, searchKey, locationId, role));
    }

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.member.listAllMembers}")
    @GetMapping("${api.endpoints.member.listAllMembers}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = UserInfoForDropDown.class)))},
            description = "${api.swagger.schema-description.pagination}")
    public ResponseEntity<PaginationDto> listAllMembers(@PathVariable int pageNo, @PathVariable int pageSize,
                                                        @Parameter(description = "${api.swagger.param-description.getUsersSearchKey}")
                                                        @RequestParam(required = false) String searchKey,
                                                        @RequestParam String locationRoleId) {
        return ResponseEntity.ok(memberService.listAllUsers(pageNo, pageSize, searchKey, locationRoleId));
    }

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.member.getMemberById}")
    @GetMapping("${api.endpoints.member.getMemberById}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = UserDetailsForAdminDto.class))})
    public ResponseEntity<UserDetailsForAdminDto> getMemberById(@RequestParam String userId) {
        return ResponseEntity.ok(memberService.getMemberById(userId));
    }

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.member.addEmployeePreferences}")
    @PostMapping("${api.endpoints.member.addEmployeePreferences}")
    public void addEmployeePreferences(@Valid @RequestBody EmployeePreferencesDto employeePreferencesDto) {
        memberService.addEmployeePreferences(employeePreferencesDto);
    }

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.member.getEmployeePreferences}")
    @GetMapping("${api.endpoints.member.getEmployeePreferences}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = EmployeePreferencesDto.class)))},
            description = "${api.swagger.schema-description.getEmployeePreferences}")
    public ResponseEntity<Object> getEmployeePreferences(@Parameter(description = "${api.swagger.param-description.getEmployeePreferences}")
                                                         @RequestParam boolean getFullHistory,
                                                         @Parameter(description = "${api.swagger.param-description.getPreferencesUserId}")
                                                         @RequestParam(required = false) String userId) {
        return ResponseEntity.ok(memberService.getEmployeePreferences(userId, getFullHistory));
    }

}