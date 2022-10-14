package ca.waaw.dto;

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
@ValidateDependentDtoField(type = DependentDtoFieldsValidatorType.TIME_OFF_USER_ID_TO_USER_ROLE,
        message = "userId are required for Admin and Manager role.")
public class NewTimeOffDto {

    @Valid
    @NotNull
    private DateTimeDto startDate;

    @Valid
    @NotNull
    private DateTimeDto endDate;

    private String note;

    @Schema(description = "Send in case of admin trying to add manually")
    private String userId;

}