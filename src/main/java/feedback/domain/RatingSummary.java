package feedback.domain;

import feedback.domain.exception.NoRatingToWithdrawException;
import feedback.domain.exception.RatingDoesNotFitSummaryException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The averages shown for one provider or one helper: how many ratings, the average stars, and for a
 * provider the average of each category, or for a helper how often each compliment was given. A hidden
 * rating is withdrawn so it no longer counts.
 */
public final class RatingSummary {

    /** Whom the summary is about. Providers get category averages; helpers get compliments. */
    public enum Subject {
        PROVIDER,
        HELPER
    }

    private final Subject subject;
    private final String subjectId;
    private int count;
    private long starsTotal;
    private final Map<ProviderCategory, Tally> categories = new EnumMap<>(ProviderCategory.class);
    private final Map<Compliment, Integer> compliments = new EnumMap<>(Compliment.class);

    private RatingSummary(Subject subject, String subjectId) {
        this.subject = subject;
        this.subjectId = subjectId;
    }

    public static RatingSummary forProvider(String providerId) {
        return new RatingSummary(Subject.PROVIDER, FeedbackText.require(providerId, "A provider id"));
    }

    public static RatingSummary forHelper(String helperId) {
        return new RatingSummary(Subject.HELPER, FeedbackText.require(helperId, "A helper id"));
    }

    /** Counts one rating. */
    public void record(Stars stars, Map<ProviderCategory, Stars> categoryStars, Set<Compliment> given) {
        requireFits(categoryStars, given);
        count++;
        starsTotal += stars.value();
        categoryStars.forEach((category, rated) -> categories.computeIfAbsent(category, any -> new Tally()).add(rated.value()));
        given.forEach(compliment -> compliments.merge(compliment, 1, Integer::sum));
    }

    /** Takes back a rating that was counted, for example one Homa hid. */
    public void withdraw(Stars stars, Map<ProviderCategory, Stars> categoryStars, Set<Compliment> given) {
        requireFits(categoryStars, given);
        boolean recorded = count > 0
                && categoryStars.keySet().stream().allMatch(category -> categories.containsKey(category))
                && given.stream().allMatch(compliments::containsKey);
        if (!recorded) {
            throw new NoRatingToWithdrawException(subject.name().toLowerCase() + " " + subjectId);
        }
        count--;
        starsTotal -= stars.value();
        categoryStars.forEach((category, rated) -> categories.get(category).remove(rated.value()));
        categories.values().removeIf(Tally::isEmpty);
        given.forEach(compliment -> compliments.merge(compliment, -1, Integer::sum));
        compliments.values().removeIf(times -> times == 0);
    }

    /** The average stars, to one decimal place, rounded half-up. Zero before any rating. */
    public BigDecimal averageStars() {
        return average(starsTotal, count);
    }

    /** The average of one category, counting only the ratings that gave it. Empty if none did. */
    public Optional<BigDecimal> categoryAverage(ProviderCategory category) {
        return Optional.ofNullable(categories.get(category)).map(tally -> average(tally.total, tally.count));
    }

    /** Whether the average is shown yet: only from the minimum ratings to show. Until then it shows as "New". */
    public boolean isShown(FeedbackLimits limits) {
        return count >= limits.minimumRatingsToShow();
    }

    /** The most-given compliments, most first; ties keep the order compliments are listed in. */
    public List<Compliment> topCompliments(int n) {
        return compliments.entrySet().stream()
                .sorted(Map.Entry.<Compliment, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(n)
                .map(Map.Entry::getKey)
                .toList();
    }

    public int complimentCount(Compliment compliment) {
        return compliments.getOrDefault(compliment, 0);
    }

    public int ratingCount() {
        return count;
    }

    public Subject subject() {
        return subject;
    }

    public String subjectId() {
        return subjectId;
    }

    private void requireFits(Map<ProviderCategory, Stars> categoryStars, Set<Compliment> given) {
        if (subject == Subject.HELPER && !categoryStars.isEmpty()) {
            throw new RatingDoesNotFitSummaryException("Category ratings are for providers, not helpers");
        }
        if (subject == Subject.PROVIDER && !given.isEmpty()) {
            throw new RatingDoesNotFitSummaryException("Compliments are for helpers, not providers");
        }
    }

    private static BigDecimal average(long total, int count) {
        if (count == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(total).divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
    }

    /** Running count and total for one category. */
    private static final class Tally {
        private int count;
        private long total;

        void add(int stars) {
            count++;
            total += stars;
        }

        void remove(int stars) {
            count--;
            total -= stars;
        }

        boolean isEmpty() {
            return count == 0;
        }
    }
}
