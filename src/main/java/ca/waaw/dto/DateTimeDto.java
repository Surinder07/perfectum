package ca.waaw.dto;

import ca.waaw.web.rest.utils.customannotations.ValidateRegex;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.RegexValidatorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateTimeDto {

    @ValidateRegex(type = RegexValidatorType.DATE, message = "Pass a valid date")
    @Schema(description = "Date Format: <b>${api.date-format}</b>")
    private String date;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>${api.time-format}</b>")
    private String time;

}
