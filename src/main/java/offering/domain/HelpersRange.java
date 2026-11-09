package offering.domain;

import offering.domain.exception.InvalidHelpersRangeException;
import java.util.Optional;

/**
 * The minimum and maximum number of helpers a customer can book with this provider. Homa sets no upper
 * limit; the maximum is capped by the helpers on the team.
 */
public record HelpersRange(int min, int max) {

    public HelpersRange {
        if (min < 1 || min > max) {
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
     * The range for a team with {@code helpers} helpers: the maximum shrinks to match, and the minimum with
     * it if needed. A team with no helper has no range.
     */
    Optional<HelpersRange> fittedTo(int helpers) {
        if (helpers < 1) {
            return Optional.empty();
        }
        return Optional.of(new HelpersRange(Math.min(min, helpers), Math.min(max, helpers)));
    }
}
