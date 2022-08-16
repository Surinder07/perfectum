package ca.waaw.dto.userdtos;

import ca.waaw.enumration.AccountMessagesType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountMessagesDto {

    private String title;
    private String description;
    private AccountMessagesType type;

}
