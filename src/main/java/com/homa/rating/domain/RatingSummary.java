package com.homa.rating.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What the revealed ratings about one party add up to (RTG-6): how many there are, the average of their stars, and
 * the average on each category that at least one rating gave stars on. Averages are shown to one decimal place,
 * rounded half up, so Kemas Home's ratings of 5, 3 and 3 show 3.7.
 */
public final class RatingSummary {

    private static final int DECIMALS = 1;

    private final int ratingCount;
    private final BigDecimal averageStars;
    private final Map<RatingCategory, BigDecimal> categoryAverages;

    private RatingSummary(int ratingCount, BigDecimal averageStars, Map<RatingCategory, BigDecimal> categoryAverages) {
        this.ratingCount = ratingCount;
        this.averageStars = averageStars;
        this.categoryAverages = Collections.unmodifiableMap(categoryAverages);
    }

    /** The summary of these ratings, every one about the same party. */
    public static RatingSummary of(List<Rating> ratings) {
        Map<RatingCategory, BigDecimal> categoryAverages = new EnumMap<>(RatingCategory.class);
        for (RatingCategory category : RatingCategory.values()) {
            List<Stars> given = ratings.stream().map(rating -> rating.categoryStars().get(category))
                    .filter(stars -> stars != null).toList();
            average(given).ifPresent(average -> categoryAverages.put(category, average));
        }
        List<Stars> overall = ratings.stream().map(Rating::stars).toList();
        return new RatingSummary(ratings.size(), average(overall).orElse(null), categoryAverages);
    }

    private static Optional<BigDecimal> average(List<Stars> stars) {
        if (stars.isEmpty()) {
            return Optional.empty();
        }
        int total = stars.stream().mapToInt(Stars::value).sum();
        return Optional.of(BigDecimal.valueOf(total).divide(BigDecimal.valueOf(stars.size()), DECIMALS,
                RoundingMode.HALF_UP));
    }

    /** How many revealed ratings stand. */
    public int ratingCount() {
        return ratingCount;
    }

    /** The average of the overall stars; empty when there are no ratings. */
    public Optional<BigDecimal> averageStars() {
        return Optional.ofNullable(averageStars);
    }

    /** The average on one category; empty when no rating gave stars on it. */
    public Optional<BigDecimal> averageOf(RatingCategory category) {
        return Optional.ofNullable(categoryAverages.get(category));
    }
}
