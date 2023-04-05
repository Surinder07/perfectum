package ca.waaw.enumration;

/**
 * Custom error codes for the tasks that may need some action on frontend
 */
public enum ErrorCodes {
    WE_001("Profile completion is pending"),
    WE_002("Payment Info is pending"),
    WE_003("Payment is pending");

    public final String value;

    ErrorCodes(final String value) {
        this.value = value;
    }

}
