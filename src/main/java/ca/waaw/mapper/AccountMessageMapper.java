package ca.waaw.mapper;

import ca.waaw.domain.joined.UserOrganization;
import ca.waaw.dto.userdtos.AccountMessagesDto;
import ca.waaw.dto.userdtos.UserDetailsDto;
import ca.waaw.enumration.AccountMessagesType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class AccountMessageMapper {

    /**
     * Will check if trial is not over yet and add a warning message to display about remaining trial period
     *
     * @param target User Details Dto to be sent to frontend
     * @param source User Organization entity fetched from database
     */
    public static void checkTrialAndAddWarning(UserDetailsDto target, UserOrganization source) {
        Instant createdDate = source.getOrganization().getCreatedDate();
        int trialDays = source.getOrganization().getTrialDays();
        if (createdDate.isAfter(Instant.now().minus(trialDays, ChronoUnit.DAYS))) {
            int daysRemaining = (int) ChronoUnit.DAYS.between(createdDate.plus(trialDays, ChronoUnit.DAYS), Instant.now());
            AccountMessagesDto messagesDto = new AccountMessagesDto();
            messagesDto.setTitle("Trial will run out soon");
            messagesDto.setDescription(String.format("Only %s remaining in your trial. Buy a plan to continue using services without interruption",
                    daysRemaining));
            messagesDto.setType(AccountMessagesType.WARNING);
            target.getAccountMessages().add(messagesDto);
        }
    }

}
