package offering.domain;

import offering.domain.exception.InvalidHelpersRangeException;
import java.util.Optional;

/** The minimum and maximum number of helpers a customer can book with this provider. */
public record HelpersRange(int min, int max) {

    public HelpersRange {
        if (min > max) {
            throw new InvalidHelpersRangeException(min, max);
        }
    }

    public static HelpersRange of(int min, int max) {
        return new HelpersRange(min, max);
    }

    public boolean contains(int helpers) {
        return helpers >= min && helpers <= max;
    }

    /**
     * The range for a team of {@code teamSize} verified helpers: the maximum shrinks to the team size,
     * and the minimum with it if needed. A team of no one has no range.
     */
    Optional<HelpersRange> fittedTo(int teamSize) {
        if (teamSize < 1) {
            return Optional.empty();
        }
        return Optional.of(new HelpersRange(Math.min(min, teamSize), Math.min(max, teamSize)));
    }
}
