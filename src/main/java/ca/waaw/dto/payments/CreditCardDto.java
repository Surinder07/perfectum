package ca.waaw.dto.payments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardDto {

    private String id;

    private String lastFourDigits;

    private String brand;

    private boolean cvcChecked;

    private int expiryMonth;

    private int expiryYear;

    private boolean isDefault;

}