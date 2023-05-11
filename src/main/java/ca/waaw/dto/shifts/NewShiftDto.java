package ca.waaw.dto.shifts;

import ca.waaw.dto.DateTimeDto;
import ca.waaw.web.rest.utils.customannotations.CapitalizeFirstLetter;
import ca.waaw.web.rest.utils.customannotations.ToUppercase;
import ca.waaw.web.rest.utils.customannotations.ValidateDependentDtoField;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.DependentDtoFieldsValidatorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidateDependentDtoField(type = DependentDtoFieldsValidatorType.NEW_SHIFT_REQUIRED_FIELD,
        message = "Pass all required values")
public class NewShiftDto {

    @NotEmpty
    @ToUppercase
    @Schema(allowableValues = {"SINGLE", "BATCH"})
    private String type;

    @CapitalizeFirstLetter
    private String shiftName;

    @Valid
    @NotNull
    private DateTimeDto start;

    @Valid
    @NotNull
    private DateTimeDto end;

    private List<String> userIds;

    private String locationId;

    private List<String> locationRoleIds;

    private String notes;

    @Schema(description = "Send true if the shift is to be immediately released to employees")
    private boolean instantRelease;

}