package ca.waaw.dto.locationandroledtos;

import ca.waaw.web.rest.utils.customannotations.CapitalizeFirstLetter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationRoleDetailedDto {

    private String id;

    private String waawId;

    @CapitalizeFirstLetter
    private String name;

    @CapitalizeFirstLetter
    private String location;

    private String createdBy;

    private String creationDate;

    private boolean isActive;

    private boolean admin;
}