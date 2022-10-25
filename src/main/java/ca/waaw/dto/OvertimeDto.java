package ca.waaw.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OvertimeDto {

    @Valid
    @NotNull
    private DateTimeDto start;

    @Valid
    @NotNull
    private DateTimeDto end;

    @Schema(description = "Required only for admins trying to add overtime manually")
    private String userId;

    private String note;

}