package ca.waaw;

import ca.waaw.config.applicationconfig.AppUrlConfig;
import ca.waaw.domain.User;
import ca.waaw.domain.joined.UserOrganization;
import ca.waaw.repository.UserOrganizationRepository;
import ca.waaw.service.NotificationInternalService;
import ca.waaw.service.UserMailService;
import ca.waaw.web.rest.utils.CommonUtils;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/test")
@AllArgsConstructor
public class ApiTestingController {

    private final AppUrlConfig appUrlConfig;

    private final UserMailService userMailService;

    private final UserOrganizationRepository userRepository;

    private final NotificationInternalService notificationInternalService;

    @GetMapping("/1")
    public void test1() {
        String activationUrl = appUrlConfig.getActivateAccountUrl(CommonUtils.Random.generateRandomKey());
        User user = new User();
        user.setFirstName("Akhil");
        user.setEmail("akhilchauhan04@gmail.com");
        userMailService.sendActivationEmail(user, activationUrl);
    }

    @GetMapping("/2")
    public ResponseEntity<UserOrganization> test2() {
        try {
            UserOrganization user = userRepository.findOneByUsernameAndDeleteFlag("waaw2020", false)
                    .orElseThrow(Exception::new);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body(null);
        }
    }

    @GetMapping("/3")
    public void test3() {
        try {
            UserOrganization user = userRepository.findOneByUsernameAndDeleteFlag("waaw2020", false)
                    .orElseThrow(Exception::new);
            User user2 = new User();
            user2.setId("4e398d05-2a5b-4aca-b14d-e1717a1a388f");
            user2.setFirstName("Akhil");
            user2.setLastName("Chauhan");
            user2.setUsername("waaw2020");
            user2.setEmail("akhilchauhan04@gmail.com");
            user2.setEmailNotifications(true);
            notificationInternalService.notifyAdminAboutNewUser(user, user2, "http://google.com");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
