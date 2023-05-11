package ca.waaw.web.rest;

import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.payments.PromotionCodeDto;
import ca.waaw.web.rest.service.SuperUserService;
import ca.waaw.web.rest.utils.customannotations.swagger.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.super-user}", description = "APIs for an additional role i.e. SUPER_USER (application admin)")
public class SuperUserController {

    private final SuperUserService superUserService;

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerAlreadyExist
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.super-user.addCode}")
    @PostMapping("${api.endpoints.super-user.addCode}")
    public void addNewCode(@Valid @RequestBody PromotionCodeDto promotionCodeDto) {
        try {
            superUserService.addNewCode(promotionCodeDto);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.super-user.deleteCode}")
    @DeleteMapping("${api.endpoints.super-user.deleteCode}")
    public void deleteCode(@RequestParam String id) {
        superUserService.deleteCode(id);
    }

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.super-user.getAllCodes}")
    @GetMapping("${api.endpoints.super-user.getAllCodes}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = PromotionCodeDto.class)))},
            description = "${api.swagger.schema-description.pagination}")
    public ResponseEntity<PaginationDto> getAllCodes(@PathVariable int pageNo, @PathVariable int pageSize,
                                                     @Parameter(description = "${api.swagger.param-description.getPromoCodeIncludeDeleted}")
                                                     @RequestParam(required = false) boolean includeDeleted,
                                                     @Parameter(description = "${api.swagger.param-description.getPromoCodeIncludeExpired}")
                                                     @RequestParam(required = false) boolean includeExpired) {
        return ResponseEntity.ok(superUserService.getAllCodes(pageNo, pageSize, includeDeleted, includeExpired));
    }

}