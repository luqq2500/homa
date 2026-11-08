package offering.domain;

import offering.domain.exception.InvalidHomaLimitsException;

/**
 * The Homa-set ranges a listing must sit within: the duration range in hours and the helpers per
 * search. At launch they are 1 to 6 hours and 1 to 10 helpers.
 */
public record HomaLimits(int minHours, int maxHours, int minHelpers, int maxHelpers) {

    public HomaLimits {
        requireRange("Homa's duration range", minHours, maxHours);
        requireRange("Homa's helpers per search", minHelpers, maxHelpers);
    }

    private static void requireRange(String what, int min, int max) {
        if (min < 1 || min > max) {
            throw new InvalidHomaLimitsException(what, min, max);
        }
    }
}
