package ca.waaw.web.rest;

import ca.waaw.dto.ApiResponseMessageDto;
import ca.waaw.dto.EmployeePreferencesDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.userdtos.InviteUserDto;
import ca.waaw.dto.userdtos.UpdateUserDto;
import ca.waaw.dto.userdtos.UserDetailsForAdminDto;
import ca.waaw.dto.userdtos.UserListingDto;
import ca.waaw.web.rest.service.MemberService;
import ca.waaw.web.rest.utils.customannotations.swagger.*;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

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

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.member.resendInvite}")
    @GetMapping("${api.endpoints.member.resendInvite}")
    public void resendInvite(@RequestParam String userId) {
        memberService.resendInvite(userId);
    }

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.member.sendInviteByUpload}")
    @PostMapping("${api.endpoints.member.sendInviteByUpload}")
    public ResponseEntity<ApiResponseMessageDto> sendInviteByUpload(@RequestPart MultipartFile file) {
        return ResponseEntity.ok(memberService.inviteNewUsersByUpload(file));
    }

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.member.getAllMembers}")
    @GetMapping("${api.endpoints.member.getAllMembers}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = UserListingDto.class)))},
            description = "${api.swagger.schema-description.pagination}")
    public ResponseEntity<PaginationDto> getAllMembers(@PathVariable int pageNo, @PathVariable int pageSize,
                                                       @RequestParam(required = false) String searchKey,
                                                       @RequestParam(required = false) String locationId,
                                                       @RequestParam(required = false) String roleId,
                                                       @RequestParam(required = false) String type,
                                                       @RequestParam(required = false) String status) {
        // TODO Add sort
        if (StringUtils.isEmpty(searchKey)) searchKey = null;
        if (StringUtils.isEmpty(locationId)) locationId = null;
        if (StringUtils.isEmpty(roleId)) roleId = null;
        if (StringUtils.isEmpty(type)) type = null;
        if (StringUtils.isEmpty(status)) status = null;
        return ResponseEntity.ok(memberService.getAllUsers(pageNo, pageSize, searchKey, locationId, roleId,
                type, status));
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

    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.member.updateMember}")
    @PostMapping("${api.endpoints.member.updateMember}")
    public void updateMemberDetails(@Valid @RequestBody UpdateUserDto memberDto) {
        memberService.updateMember(memberDto);
    }

}