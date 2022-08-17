package ca.waaw.dto.locationandroledtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeLocationDto {

    private String id;

    private String name;

    private String timezone;

    private LocationRoleDto locationRoles;

}