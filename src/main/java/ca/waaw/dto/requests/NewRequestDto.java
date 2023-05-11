package ca.waaw.dto.requests;

import ca.waaw.dto.DateTimeDto;
import ca.waaw.enumration.request.RequestSubType;
import ca.waaw.enumration.request.RequestType;
import ca.waaw.web.rest.utils.customannotations.ToUppercase;
import ca.waaw.web.rest.utils.customannotations.ValueOfEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewRequestDto {

    @ToUppercase
    @ValueOfEnum(enumClass = RequestType.class, message = "Please provide a valid type")
    private String type;

    @ToUppercase
    @ValueOfEnum(enumClass = RequestSubType.class, message = "Please provide a valid type")
    private String subType;

    private DateTimeDto start;

    private String endDate;

    private int duration;

    @NotEmpty
    private String description;

}