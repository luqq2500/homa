package offering.domain;

import offering.domain.exception.InvalidHomaLimitsException;

/** The Homa-set duration range a listing's hours must sit within: 1 to 6 hours at launch. Homa sets no limit on helpers. */
public record HomaLimits(int minHours, int maxHours) {

    public HomaLimits {
        if (minHours < 1 || minHours > maxHours) {
            throw new InvalidHomaLimitsException("Homa's duration range", minHours, maxHours);
        }
    }
}
