package offering.domain;

import java.util.Optional;
import offering.domain.exception.InvalidHelperRangeException;

/** The fewest and the most helpers a customer can book. Homa sets no upper limit. */
public record HelperRange(int min, int max) {

    public HelperRange {
        if (min < 1) {
            throw new InvalidHelperRangeException("A booking has at least 1 helper");
        }
        if (max < min) {
            throw new InvalidHelperRangeException("The maximum helpers cannot be fewer than the minimum");
        }
    }

    /**
     * The range once the team has this many helpers: the maximum shrinks to match, and the minimum
     * with it if needed. With no helper left there is no range. A larger team does not widen it.
     */
    public Optional<HelperRange> shrunkTo(int helpersOnTeam) {
        if (helpersOnTeam < 1) {
            return Optional.empty();
        }
        int newMax = Math.min(max, helpersOnTeam);
        return Optional.of(new HelperRange(Math.min(min, newMax), newMax));
    }
}
