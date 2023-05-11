package ca.waaw.domain.payments;

import ca.waaw.enumration.payment.PromoCodeType;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "promotion_codes")
@NamedQuery(name = "PromotionCode.getAllByConditionalExpiredAndDeleted", query = "SELECT c FROM PromotionCode c" +
        " WHERE (?1 = true OR c.deleteFlag = false) AND (?2 = true or c.expiryDate > NOW())")
public class PromotionCode implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Builder.Default
    @Column(name = "uuid")
    private String id = UUID.randomUUID().toString();

    @Column
    @Enumerated(EnumType.STRING)
    private PromoCodeType type;

    @Column
    private String code;

    @Column(name = "promotion_value")
    private int promotionValue;

    @Column(name = "expiry_date")
    private Instant expiryDate;

    @Builder.Default
    @Column(name = "del_flg")
    private boolean deleteFlag = false;

    @Builder.Default
    @Column(name = "created_date")
    private Instant createdDate = Instant.now();

}