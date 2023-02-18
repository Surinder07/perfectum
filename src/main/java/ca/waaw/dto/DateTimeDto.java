package ca.waaw.dto;

import ca.waaw.web.rest.utils.customannotations.ValidateRegex;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.RegexValidatorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateTimeDto {

    @ValidateRegex(type = RegexValidatorType.DATE, message = "Pass a valid date")
    @Schema(description = "Date Format: <b>yyyy-MM-dd</b>")
    private String date;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String time;

    public String toString() {
        return date + " " + time;
    }

}
