package ca.waaw;

import ca.waaw.config.applicationconfig.AppUrlConfig;
import ca.waaw.domain.User;
import ca.waaw.dto.LoginDto;
import ca.waaw.email.javamailsender.user.UserMailService;
import ca.waaw.web.rest.utils.CommonUtils;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/test")
@AllArgsConstructor
public class ApiTestingController {

    private final AppUrlConfig appUrlConfig;

    private final UserMailService userMailService;

    @GetMapping("/1")
    public void test1() {
        String activationUrl = appUrlConfig.getActivateAccountUrl(CommonUtils.Random.generateRandomKey());
        User user = new User();
        user.setFirstName("Akhil");
        user.setEmail("akhilchauhan04@gmail.com");
        userMailService.sendActivationEmail(user, activationUrl);
    }

}
