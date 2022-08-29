package ca.waaw.dto.shifts;

import ca.waaw.web.rest.utils.customannotations.ValidateDependentDtoField;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.DependentDtoFieldsValidatorType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidateDependentDtoField(type = DependentDtoFieldsValidatorType.SHIFT_USER_ID_TO_LOCATION_ROLE_ID,
        message = "Pass user_id to assign shift or location_role_id for an unassigned shift")
public class NewShiftDto {

    private String userId;

    @NotNull
    private Instant start;

    @NotNull
    private Instant end;

    private String notes;

    private String locationRoleId;

}