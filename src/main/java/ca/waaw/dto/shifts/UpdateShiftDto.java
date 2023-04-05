package ca.waaw.dto.shifts;

import ca.waaw.dto.DateTimeDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShiftDto {

    @Schema(description = "Required for assigned shifts")
    private String id;

    @Valid
    @NotNull
    private DateTimeDto start;

    @Valid
    @NotNull
    private DateTimeDto end;

    private String comments;

}
