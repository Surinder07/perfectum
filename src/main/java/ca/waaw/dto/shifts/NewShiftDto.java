package ca.waaw.dto.shifts;

import ca.waaw.dto.DateTimeDto;
import ca.waaw.web.rest.utils.customannotations.ValidateDependentDtoField;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.DependentDtoFieldsValidatorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidateDependentDtoField(type = DependentDtoFieldsValidatorType.SHIFT_USER_ID_TO_LOCATION_ROLE_ID,
        message = "Pass userId to assign shift or locationRoleId for an unassigned shift")
public class NewShiftDto {

    @Schema(description = "Required for assigned shifts")
    private String userId;

    @Valid
    @NotNull
    private DateTimeDto start;

    @Valid
    @NotNull
    private DateTimeDto end;

    private String notes;

    private String locationRoleId;

    @Schema(description = "If userId is not passed <b>(i.e. Shift is unassigned), send true if anyone can claim the " +
            "shift, and false if admin approval is required.</b>")
    private boolean assignToFirstClaim;

    @Schema(description = "Send true if the shift is to be immediately released to employees")
    private boolean instantRelease;


}