package ca.waaw.dto.payments;

import ca.waaw.enumration.payment.PromoCodeType;
import ca.waaw.web.rest.utils.customannotations.ToUppercase;
import ca.waaw.web.rest.utils.customannotations.ValueOfEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionCodeDto {

    private String id;

    @NotEmpty
    @Schema(allowableValues = {"TRIAL"})
    @ValueOfEnum(enumClass = PromoCodeType.class, message = "Pass a valid promo type")
    private String type;

    @NotEmpty
    @ToUppercase
    @Size(max = 6, message = "Please enter a code with 6 literals")
    private String code;

    @NotNull
    @Schema(description = "No. of trial days, discount value, etc.")
    private int promotionValue;

    @Schema(description = "Code will expire after how many days, leave null for <b>never</b>")
    private Integer expireAfterDays;

    private boolean isExpired = false;

    private boolean isDeleted = false;

}
