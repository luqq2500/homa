package reservation.domain;

import java.time.Instant;

/** How the provider's business stands with the KYC provider: the date it is verified until, and any serious flag. */
public record ProviderStanding(String providerId, Instant verifiedUntil, boolean seriouslyFlagged) {

    public static ProviderStanding unverified(String providerId) {
        return new ProviderStanding(providerId, null, false);
    }

    /** A business can be requested, and can accept, only for a job that ends by its verified-until date. */
    public boolean isVerifiedFor(Instant jobEnd) {
        return !seriouslyFlagged && verifiedUntil != null && !jobEnd.isAfter(verifiedUntil);
    }
}
