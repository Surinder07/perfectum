package ca.waaw.dto.reports;

import ca.waaw.enumration.report.UserReport;
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
public class GenerateReportDto {

    private String locationId;

    @NotEmpty
    @ValueOfEnum(enumClass = UserReport.class)
    @Schema(description = "Allowed values: PAYROLL, ATTENDANCE, HOLIDAYS")
    private String reportType;

    @NotEmpty
    @ValidateRegex(type = RegexValidatorType.DATE)
    private String startDate;

    @NotEmpty
    @ValidateRegex(type = RegexValidatorType.DATE)
    private String endDate;

}