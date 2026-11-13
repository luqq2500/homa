package payment.domain;

/** Where a payout stands. Only a {@code PAYABLE} payout is paid out to the provider. */
public enum PayoutStatus {
    /** Ready to be paid out. */
    PAYABLE,
    /** On hold because the provider's business was seriously flagged, waiting for Homa's admins. */
    HELD,
    /** The admins kept it back after their review, with a reason, for example a sanctions match. */
    WITHHELD
}
