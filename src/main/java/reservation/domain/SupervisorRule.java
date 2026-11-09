package reservation.domain;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * When the provider sends a supervisor, as the reservation copied it: from a minimum number of helpers, from
 * a minimum number of hours, or both.
 */
public record SupervisorRule(OptionalInt fromHelpers, OptionalInt fromHours) {

    public SupervisorRule {
        Objects.requireNonNull(fromHelpers);
        Objects.requireNonNull(fromHours);
    }

    /** Whether a booking of {@code helpers} for {@code hours} reaches either threshold. */
    public boolean requiresSupervisor(int helpers, int hours) {
        return reaches(fromHelpers, helpers) || reaches(fromHours, hours);
    }

    private static boolean reaches(OptionalInt threshold, int value) {
        return threshold.isPresent() && value >= threshold.getAsInt();
    }
}
