package ca.waaw.dto.shifts;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchDetailsDto {

    private String id;

    private String waawId;

    private String name;

    @Schema(description = "Date Format: <b>yyyy/MM/dd</b>")
    private String startDate;

    @Schema(description = "Date Format: <b>yyyy/MM/dd</b>")
    private String endDate;

    private String status;

    private String creationDate;

    private List<ShiftDetailsDto> shifts;

}
