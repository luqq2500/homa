package offering.domain;

/** Something a provider must have before their listing can go live. */
public enum GoLiveRequirement {
    PROFILE("profile"),
    PROFILE_PICTURE("profile picture"),
    JOBS("jobs"),
    PRICING("pricing"),
    HOURS("hours"),
    HELPERS("helpers"),
    SUPERVISOR("a supervisor on the team, for the supervisor rule"),
    CITIES("cities"),
    CANCELLATION_TIER("cancellation tier"),
    PROVIDER_VERIFIED_AND_PAYOUT_READY("provider verified with a ready payout account, and a business registration checked");

    private final String description;

    GoLiveRequirement(String description) {
        this.description = description;
    }

    /** How the requirement is shown to the provider, for example "profile picture". */
    public String description() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
