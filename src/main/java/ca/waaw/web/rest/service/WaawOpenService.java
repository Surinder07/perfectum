package ca.waaw.web.rest.service;

import ca.waaw.domain.SubscriptionList;
import ca.waaw.repository.SubscriptionListRepository;
import ca.waaw.web.rest.errors.exceptions.application.AlreadySubscribedException;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class WaawOpenService {

    private final Logger log = LogManager.getLogger(WaawOpenService.class);

    private final SubscriptionListRepository subscriptionListRepository;

    public void subscribeEmail(String email) {
        SubscriptionList subscriptionEntity = subscriptionListRepository.findOneByEmail(email)
                .map(subscription -> {
                    if (subscription.isSubscriptionActive()) {
                        throw new AlreadySubscribedException();
                    }
                    subscription.setSubscriptionActive(true);
                    return subscription;
                })
                .orElse(null);
        if (subscriptionEntity == null) {
            subscriptionEntity = new SubscriptionList();
            subscriptionEntity.setEmail(email);
        }
        subscriptionListRepository.save(subscriptionEntity);
        log.info("Successfully added '{}' to subscription list", email);
    }

}