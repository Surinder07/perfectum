package ca.waaw.dto.userdtos;

import ca.waaw.enumration.AccountMessagesType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountMessagesDto {

    private String message;
    @Schema(description = "Use these to show different colors")
    private AccountMessagesType type;
    @Schema(description = "Show this message after message with a hyperlink to actionUrl")
    private String actionMessage;
    private String actionUrl;

}
