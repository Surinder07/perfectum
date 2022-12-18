package ca.waaw.dto.locationandroledtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationDetailedDto {

    private String id;

    private String waawId;

    private String name;

    private String timezone;

    private String creationDate;

    private int activeEmployees;

    private int inactiveEmployees;

    private boolean isActive;

}