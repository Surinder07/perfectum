package ca.waaw.dto;

import ca.waaw.web.rest.utils.customannotations.CapitalizeFirstLetter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardDto {

    @NotEmpty
    @CapitalizeFirstLetter
    private String firstName;

    @NotEmpty
    @CapitalizeFirstLetter
    private String lastName;

    private String cardNumber;

    @Size(max = 7, message = "Please enter a valid expiry date")
    private String expiry;

    @Size(min = 3, max = 3, message = "Please enter a valid postal code")
    private String securityCode;

    private String country;

    @Size(min = 5, max = 6, message = "Please enter a valid postal code")
    private String postalCode;

    private String address;

}