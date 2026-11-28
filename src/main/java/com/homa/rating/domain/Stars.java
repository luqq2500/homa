package com.homa.rating.domain;

import com.homa.rating.domain.exception.InvalidStarsException;

/** One star score, from {@link RatingValues#MINIMUM_STARS} to {@link RatingValues#MAXIMUM_STARS} (RTG-4). */
public record Stars(int value) {

    public Stars {
        if (value < RatingValues.MINIMUM_STARS || value > RatingValues.MAXIMUM_STARS) {
            throw new InvalidStarsException(value);
        }
    }
}
