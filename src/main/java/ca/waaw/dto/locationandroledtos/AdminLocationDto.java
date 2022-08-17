package ca.waaw.dto.locationandroledtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminLocationDto {

    private String id;

    private String name;

    private String timezone;

    private List<LocationRoleDto> locationRoles;

}