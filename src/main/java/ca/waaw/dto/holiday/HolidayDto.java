package ca.waaw.dto.holiday;

import ca.waaw.enumration.HolidayType;
import ca.waaw.web.rest.utils.customannotations.ValueOfEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HolidayDto {

    private String id;

    private String locationId;

    @NotEmpty
    private String name;

    @Schema(allowableValues = {"NATIONAL_HOLIDAY", "ORGANIZATION_HOLIDAY"})
    @ValueOfEnum(enumClass = HolidayType.class)
    private String type;

    @Min(value = 2021, message = "Year cannot be before 2021")
    private int year;

    @Min(value = 1, message = "Month needs to be in between 1 and 12")
    @Max(value = 12, message = "Month needs to be in between 1 and 12")
    private int month;

    @Min(value = 1, message = "Date needs to be in between 1 and 31")
    @Max(value = 31, message = "Date needs to be in between 1 and 31")
    private int date;

}