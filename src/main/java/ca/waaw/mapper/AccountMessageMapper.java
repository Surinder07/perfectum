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
        AccountMessagesDto messagesDto = new AccountMessagesDto();
        if (createdDate.isAfter(Instant.now().minus(trialDays, ChronoUnit.DAYS)) && source.getOrganization().getSubscriptionPlan() == null) {
            int daysRemaining = (int) ChronoUnit.DAYS.between(createdDate.plus(trialDays, ChronoUnit.DAYS), Instant.now());
            messagesDto.setMessage(String.format("Only %s remaining in your trial. Buy a plan to continue using services without interruption",
                    daysRemaining));
            messagesDto.setType(AccountMessagesType.WARNING);
        } else if (source.getOrganization().getSubscriptionPlan() == null) {
            messagesDto.setMessage("Your trial period is over. Buy a plan to continue using services.");
            messagesDto.setType(AccountMessagesType.ERROR);
        } else return;
        messagesDto.setActionMessage("Buy a plan now");
        // TODO Add payment link here
        messagesDto.setActionUrl("");
        target.getAccountMessages().add(messagesDto);
    }

}
