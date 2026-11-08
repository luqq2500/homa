package feedback.domain;

import feedback.domain.exception.StarsOutOfRangeException;

/** A star rating: a whole number from 1 to 5. */
public record Stars(int value) {

    public Stars {
        if (value < 1 || value > 5) {
            throw new StarsOutOfRangeException(value);
        }
    }

    public static Stars of(int value) {
        return new Stars(value);
    }
}
