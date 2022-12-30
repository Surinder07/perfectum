package ca.waaw.dto.shifts;

import ca.waaw.web.rest.utils.customannotations.ValidateDependentDtoField;
import ca.waaw.web.rest.utils.customannotations.ValidateRegex;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.DependentDtoFieldsValidatorType;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.RegexValidatorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
//@ValidateDependentDtoField(type = DependentDtoFieldsValidatorType.SHIFT_BATCH_LOCATION_ID_TO_USER_ROLE,
//        message = "locationId, locationRoleId or userIds are required for Admin role.")
public class NewShiftBatchDto {

    @Schema(description = "Required for global admin only")
    private String locationId;

    @Schema(description = "If locationRoleId is passed, it will take preference and locationId will be ignored")
    private String locationRoleId;

    @Schema(description = "If userIds are passed, it will take preference and locationId/locationRoleId will be ignored")
    private List<String> userIds;

    @ValidateRegex(type = RegexValidatorType.DATE, message = "Pass a valid date")
    @Schema(description = "Date Format: <b>yyyy/MM/dd</b>")
    private String startDate;

    @ValidateRegex(type = RegexValidatorType.DATE, message = "Pass a valid date")
    @Schema(description = "Date Format: <b>yyyy/MM/dd</b>")
    private String endDate;

}
