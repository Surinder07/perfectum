package ca.waaw.domain;

import ca.waaw.enumration.DaysOfWeek;
import ca.waaw.enumration.SubscriptionPlans;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name = "organization")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Organization extends AbstractEntity {

    @Column
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "start_of_week")
    private DaysOfWeek firstDayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan")
    private SubscriptionPlans subscriptionPlan;

    @Column(name = "trial_days")
    private Integer trialDays;

    @Column(name = "is_trial_used")
    private boolean isTrialUsed;

    @Column(name = "is_paid_until")
    private Instant isPaidUntil;

    @Column(name = "waaw_custom_id")
    private String waawId;

}
