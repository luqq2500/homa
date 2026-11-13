package reservation.domain;

/** The provider's cancellation policy, as the customer sees it when they reserve. */
public enum CancellationTier {
    FLEXIBLE,
    STANDARD,
    FIRM
}
