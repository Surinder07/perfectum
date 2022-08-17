package ca.waaw.dto.locationandroledtos;

import ca.waaw.web.rest.utils.customannotations.CapitalizeFirstLetter;
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
    private String timezone;

}