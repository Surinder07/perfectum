package ca.waaw.dto.timesheet;

import ca.waaw.dto.DateTimeDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetDto {

    @Schema(description = "Required for editing timesheet")
    private String id;

    @Schema(description = "Required for new timesheet")
    private String userId;

    @Valid
    @NotNull
    private DateTimeDto start;

    @Valid
    @NotNull
    private DateTimeDto end;

    @NotEmpty
    private String comments;

}