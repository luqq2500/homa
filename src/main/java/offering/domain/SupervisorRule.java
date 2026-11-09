package offering.domain;

import offering.domain.exception.InvalidSupervisorRuleException;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * When a provider with supervisors sends one: from a minimum number of helpers, from a minimum number of
 * hours, or both. A booking that reaches either one includes at least one supervisor, at no extra cost.
 */
public record SupervisorRule(OptionalInt fromHelpers, OptionalInt fromHours) {

    public SupervisorRule {
        Objects.requireNonNull(fromHelpers);
        Objects.requireNonNull(fromHours);
        if (fromHelpers.isEmpty() && fromHours.isEmpty()) {
            throw new InvalidSupervisorRuleException("needs a minimum number of helpers, of hours, or both");
        }
        requireAtLeastOne("helpers", fromHelpers);
        requireAtLeastOne("hours", fromHours);
    }

    public static SupervisorRule fromHelpers(int helpers) {
        return new SupervisorRule(OptionalInt.of(helpers), OptionalInt.empty());
    }

    public static SupervisorRule fromHours(int hours) {
        return new SupervisorRule(OptionalInt.empty(), OptionalInt.of(hours));
    }

    public static SupervisorRule fromHelpersOrHours(int helpers, int hours) {
        return new SupervisorRule(OptionalInt.of(helpers), OptionalInt.of(hours));
    }

    /** Whether a booking of {@code helpers} for {@code hours} reaches either threshold. */
    public boolean requiresSupervisor(int helpers, int hours) {
        return reaches(fromHelpers, helpers) || reaches(fromHours, hours);
    }

    private static boolean reaches(OptionalInt threshold, int value) {
        return threshold.isPresent() && value >= threshold.getAsInt();
    }

    private static void requireAtLeastOne(String what, OptionalInt threshold) {
        if (threshold.isPresent() && threshold.getAsInt() < 1) {
            throw new InvalidSupervisorRuleException("a minimum number of " + what + " must be at least 1, was " + threshold.getAsInt());
        }
    }
}
