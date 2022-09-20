package ca.waaw.domain;

import ca.waaw.enumration.PromoCodeType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Data
@Entity
@ToString
@EqualsAndHashCode
@Table(name = "promotion_codes")
public class PromotionCode implements Serializable {

    @Id
    @Column(name = "uuid")
    private String id;

    @Column
    @Enumerated(EnumType.STRING)
    private PromoCodeType type;

    @Column
    private String code;

    @Column(name = "promotion_value")
    private int promotionValue;

    @Column(name = "expiry_date")
    private Instant expiryDate;

    @Column(name = "del_flg")
    private boolean deleteFlag = false;

    @Column(name = "created_date")
    private Instant createdDate = Instant.now();

}