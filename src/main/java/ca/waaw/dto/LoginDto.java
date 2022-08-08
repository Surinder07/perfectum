package ca.waaw.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginDto {

    @NotEmpty
    private String login;

    @NotEmpty
    private String password;

    private boolean rememberMe;

}
