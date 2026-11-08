package discovery.domain;

import java.time.Duration;
import java.util.Objects;

/** The Homa-set values a search must respect: the duration range, helpers per search, minimum notice and booking horizon. */
public record SearchLimits(int minHours, int maxHours, int minHelpers, int maxHelpers, Duration minimumNotice, Duration bookingHorizon) {

    public SearchLimits {
        Objects.requireNonNull(minimumNotice);
        Objects.requireNonNull(bookingHorizon);
    }
}
