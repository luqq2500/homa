package discovery.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * The Homa-set values a search must respect: the duration range, minimum notice and booking horizon. Homa
 * sets no limit on helpers; a search is for at least one.
 */
public record SearchLimits(int minHours, int maxHours, Duration minimumNotice, Duration bookingHorizon) {

    public SearchLimits {
        Objects.requireNonNull(minimumNotice);
        Objects.requireNonNull(bookingHorizon);
    }
}
