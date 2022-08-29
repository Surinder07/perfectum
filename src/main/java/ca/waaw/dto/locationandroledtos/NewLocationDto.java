package ca.waaw.dto.locationandroledtos;

import ca.waaw.enumration.Timezones;
import ca.waaw.web.rest.utils.customannotations.CapitalizeFirstLetter;
import ca.waaw.web.rest.utils.customannotations.ValueOfEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewLocationDto {

    @NotEmpty
    @CapitalizeFirstLetter
    private String name;

    @NotEmpty
    @ValueOfEnum(enumClass = Timezones.class, message = "Pass a valid timezone")
    private String timezone;

}