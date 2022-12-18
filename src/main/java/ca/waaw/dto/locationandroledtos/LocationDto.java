package ca.waaw.dto.locationandroledtos;

import ca.waaw.enumration.Timezones;
import ca.waaw.web.rest.utils.customannotations.CapitalizeFirstLetter;
import ca.waaw.web.rest.utils.customannotations.ValueOfEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {

    @NotEmpty
    @CapitalizeFirstLetter
    private String name;

    @NotEmpty
    @Schema(description = "Use get Timezones api to get dropdown of possible values")
    @ValueOfEnum(enumClass = Timezones.class, message = "Pass a valid timezone")
    private String timezone;

}