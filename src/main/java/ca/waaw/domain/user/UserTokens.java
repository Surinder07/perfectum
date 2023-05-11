package ca.waaw.domain.user;

import ca.waaw.enumration.user.UserTokenType;
import ca.waaw.web.rest.utils.CommonUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Table(name = "user_tokens")
public class UserTokens implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "uuid")
    private String id = UUID.randomUUID().toString();

    @Column(name = "user_id")
    private String userId;

    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type")
    private UserTokenType tokenType;

    @Column(name = "is_expired")
    private boolean isExpired;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_date")
    private Instant createdDate;

    public UserTokens(UserTokenType type) {
        this.createdDate = Instant.now();
        this.tokenType = type;
        this.token = CommonUtils.Random.generateRandomKey();
    }

}
