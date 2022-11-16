package ca.waaw.dto;

import ca.waaw.enumration.UserReport;
import ca.waaw.web.rest.utils.customannotations.ValidateRegex;
import ca.waaw.web.rest.utils.customannotations.ValueOfEnum;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.RegexValidatorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailReportDto {

    @Schema(description = "Required for Email only")
    @ValidateRegex(type = RegexValidatorType.EMAIL)
    private String email;

    @Schema(description = "Required for Email only")
    private String cc;

    @NotEmpty
    @ValueOfEnum(enumClass = UserReport.class)
    @Schema(description = "Allowed values: PAYROLL, ATTENDANCE")
    private String reportType;

    @NotEmpty
    @ValidateRegex(type = RegexValidatorType.DATE)
    private String startDate;

    @NotEmpty
    @ValidateRegex(type = RegexValidatorType.DATE)
    private String endDate;

    @NotEmpty
    @Schema(description = "Allowed formats: csv, xls")
    @ValidateRegex(type = RegexValidatorType.REPORT_FORMAT)
    private String preferredFormat;

}