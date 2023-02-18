package ca.waaw.domain;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@ToString
@EqualsAndHashCode
@Table(name = "subscription_list")
public class SubscriptionList implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "uuid")
    private String id = UUID.randomUUID().toString();

    @Column
    private String email;

    @Column(name = "subscription_active")
    private boolean subscriptionActive = true;

    @Column(name = "created_date")
    private Instant createdDate = Instant.now();

    @Column(name = "last_modified_date")
    private Instant lastModifiedDate = Instant.now();

}