package offering.domain;

import java.util.Optional;
import offering.domain.exception.InvalidHoursException;

/** A range of whole hours: a provider's own minimum and maximum, or the range Homa sets. */
public record HoursRange(int min, int max) {

    public HoursRange {
        if (min < 1) {
            throw new InvalidHoursException("A booking is at least 1 hour");
        }
        if (max < min) {
            throw new InvalidHoursException("The maximum hours cannot be fewer than the minimum");
        }
    }

    public boolean isWithin(HoursRange outer) {
        return min >= outer.min && max <= outer.max;
    }

    /** This range shrunk to fit inside another; nothing when no hours are left inside it. */
    public Optional<HoursRange> fittedInto(HoursRange outer) {
        int newMin = Math.max(min, outer.min);
        int newMax = Math.min(max, outer.max);
        return newMin > newMax ? Optional.empty() : Optional.of(new HoursRange(newMin, newMax));
    }
}
