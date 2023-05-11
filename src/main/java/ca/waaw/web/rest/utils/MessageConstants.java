package ca.waaw.web.rest.utils;

public final class MessageConstants {

    // Email Messages
    public static final String[] emailVerification = new String[]{"email.verification.title", "email.verification.content", "email.verification.action"};
    public static final String[] emailInvitation = new String[]{"email.invitation.title", "email.invitation.content", "email.invitation.action"};
    public static final String[] emailReset = new String[]{"email.reset.title", "email.reset.content", "email.reset.action"};
    public static final String[] emailInviteAccept = new String[]{"email.invite.accepted.title", "email.invite.accepted.content", "email.invite.accepted.action"};
    public static final String[] inviteAccepted=new String[]{"notification.invite.accepted.title", "notification.invite.accepted.content"};
    public static final String[] emailUpdate = new String[]{"email.update.title", "email.update.content", "email.update.action"};
    public static final String[] emailReportGenerate = new String[]{"email.report.generate.title", "email.report.generate.content", "email.report.generate.action"};
    // Shifts related messages
    public static final String[] shiftMissed = new String[]{"notification.shiftMissed.title", "notification.shiftMissed.content"};
    public static final String[] shiftMissedUser = new String[]{"notification.shiftMissed.user.title", "notification.shiftMissed.user.content"};
    public static final String[] shiftAssigned = new String[]{"notification.shiftAssigned.title", "notification.shiftAssigned.content"};
    public static final String[] shiftConflicted = new String[]{"notification.conflictingShift.title", "notification.conflictingShift.content"};
    public static final String[] shiftCreated = new String[]{"notification.shiftCreated.title", "notification.shiftCreated.content"};
    public static final String[] shiftTimeoffOverlap = new String[]{"notification.overlappingTimeoff.title", "notification.overlappingTimeoff.content"};
    public static final String[] shiftHolidayOverlap = new String[]{"notification.overlappingHoliday.title", "notification.overlappingHoliday.content"};
    public static final String[] missingEmployeePreference = new String[]{"notification.missingPreference.title", "notification.missingPreference.content"};
    public static final String[] noShiftsCreated = new String[]{"notification.noShiftsCreated.title", "notification.noShiftsCreated.content"};
    public static final String[] shiftUpdated = new String[]{"notification.shiftUpdated.title", "notification.shiftUpdated.content"};
    public static final String[] pastShiftNotDeleted = new String[]{"notification.pastShiftNotDeleted.title", "notification.pastShiftNotDeleted.content"};
    public static final String[] batchShiftsDeletedLocation = new String[]{"notification.batchShiftsDeletedLocation.title", "notification.batchShiftsDeletedLocation.content"};
    // Requests related messages
    public static final String[] newRequest = new String[]{"notification.request.new.title", "notification.request.new.content"};
    public static final String[] respondToRequest = new String[]{"notification.request.respond.title", "notification.request.respond.content"};
    public static final String[] rejectRequest = new String[]{"notification.request.reject.title", "notification.request.reject.content"};
    public static final String[] acceptRequest = new String[]{"notification.request.accept.title", "notification.request.accept.content"};
    public static final String[] pendingRequest = new String[]{"notification.request.pending.title", "notification.request.pending.content"};
    // Holiday related messages
    public static final String[] pastHolidays = new String[]{"notification.holiday.past.title", "notification.holiday.past.content"};
    public static final String[] futureHolidays = new String[]{"notification.holiday.nextYear.title", "notification.holiday.nextYear.content"};
    // File upload related messages
    public static final String[] holidaysUpload = new String[]{"notification.upload.holidays.title", "notification.upload.holidays.content"};
    public static final String[] usersUpload = new String[]{"notification.upload.users.title", "notification.upload.users.content"};
    // Payments related messages
    public static final String[] trialEnd = new String[]{"notification.trialEnd.title", "notification.trialEnd.message"};
    public static final String[] noInvoice = new String[]{"notification.noInvoice.title", "notification.noInvoice.message"};
    public static final String[] newInvoice = new String[]{"notification.newInvoice.title", "notification.newInvoice.message"};
    public static final String[] accountSuspendedMonthlyInvoice = new String[]{"email.account.suspension.title", "email.account.suspension.monthly.message"};
    public static final String[] accountSuspendedPlatformInvoice = new String[]{"email.account.suspension.title", "email.account.suspension.platform.message"};

}
