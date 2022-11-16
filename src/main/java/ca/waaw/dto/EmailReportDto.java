package ca.waaw.dto;

import ca.waaw.web.rest.utils.customannotations.ValidateRegex;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.RegexValidatorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailReportDto {

    @ValidateRegex(type = RegexValidatorType.EMAIL)
    private String email;

    private String cc;

    @ValidateRegex(type = RegexValidatorType.DATE)
    private String startDate;

    @ValidateRegex(type = RegexValidatorType.DATE)
    private String endDate;

    @Schema(description = "Allowed formats: csv, xls")
    @ValidateRegex(type = RegexValidatorType.REPORT_FORMAT)
    private String preferredFormat;

}