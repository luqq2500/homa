package offering.domain;

import offering.domain.exception.InvalidHoursRangeException;

/** The minimum and maximum whole hours a provider can be booked for. */
public record HoursRange(int min, int max) {

    public HoursRange {
        if (min > max) {
            throw new InvalidHoursRangeException(min, max);
        }
    }

    public static HoursRange of(int min, int max) {
        return new HoursRange(min, max);
    }

    public boolean contains(int hours) {
        return hours >= min && hours <= max;
    }
}
