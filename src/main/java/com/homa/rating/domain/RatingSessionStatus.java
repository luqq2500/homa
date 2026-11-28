package com.homa.rating.domain;

/** Where a {@link RatingSession} is. */
public enum RatingSessionStatus {
    /** Taking ratings, none revealed yet (RTG-3). */
    OPEN,
    /** Both ratings are in, or the Rating Window closed: whatever was rated is now shown (RTG-3). */
    REVEALED,
    /** A Provider No Show was confirmed after the job was marked Completed: the ratings are gone (NSH-6). */
    VOID
}
