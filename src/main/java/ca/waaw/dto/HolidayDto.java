package ca.waaw.dto;

import ca.waaw.enumration.HolidayType;
import ca.waaw.web.rest.utils.customannotations.ValueOfEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HolidayDto {

    private String id;

    private String locationId;

    @Schema(description = "Will be populated in case of location specific holiday")
    private String locationName;

    @NotEmpty
    private String name;

    @ValueOfEnum(enumClass = HolidayType.class)
    private String type;

    @NotEmpty
    private int year;

    @NotEmpty
    private int month;

    @NotEmpty
    private int date;

}