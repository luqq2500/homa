package feedback.domain;

import feedback.domain.exception.NoRatingToWithdrawException;
import feedback.domain.exception.RatingDoesNotFitSummaryException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What is shown for one provider or one helper: the overall count and average, and a count and average
 * for each category. A category counts only the ratings that rated it, since customers may skip
 * categories. A hidden rating is withdrawn so it no longer counts.
 */
public final class RatingSummary {

    /** Whom the summary is about, and the categories it can be rated on. */
    public enum Subject {
        PROVIDER(Arrays.stream(ProviderCategory.values()).map(Enum::name).toList()),
        HELPER(Arrays.stream(HelperCategory.values()).map(Enum::name).toList());

        private final List<String> categories;

        Subject(List<String> categories) {
            this.categories = categories;
        }

        /** The category names this subject is rated on, in the order they are listed. */
        public List<String> categories() {
            return categories;
        }
    }

    private final Subject subject;
    private final String subjectId;
    private final Tally overall = new Tally();
    private final Map<String, Tally> categories = new HashMap<>();

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

    /** Counts one rating: its overall stars, and the stars of each category it rated. */
    public void record(Stars overallStars, Map<String, Stars> categoryStars) {
        categoryStars.keySet().forEach(this::requireCategory);
        overall.add(overallStars.value());
        categoryStars.forEach((category, stars) -> categories.computeIfAbsent(category, any -> new Tally()).add(stars.value()));
    }

    /** Takes back a rating that was counted, for example one Homa hid. */
    public void withdraw(Stars overallStars, Map<String, Stars> categoryStars) {
        categoryStars.keySet().forEach(this::requireCategory);
        if (overall.count == 0 || !categories.keySet().containsAll(categoryStars.keySet())) {
            throw new NoRatingToWithdrawException(subject.name().toLowerCase() + " " + subjectId);
        }
        overall.remove(overallStars.value());
        categoryStars.forEach((category, stars) -> categories.get(category).remove(stars.value()));
        categories.values().removeIf(tally -> tally.count == 0);
    }

    /** The overall average, to one decimal place, rounded half-up. Zero before any rating. */
    public BigDecimal overallAverage() {
        return overall.average();
    }

    /** The average of one category, counting only the ratings that rated it. Zero before any did. */
    public BigDecimal categoryAverage(String category) {
        return tallyOf(category).average();
    }

    /** How many ratings rated this category. */
    public int categoryCount(String category) {
        return tallyOf(category).count;
    }

    /** Whether the overall average is shown yet: only from the minimum ratings to show. Until then it shows as "New". */
    public boolean isOverallShown(FeedbackLimits limits) {
        return overall.count >= limits.minimumRatingsToShow();
    }

    /** Whether a category's average is shown yet: once that category has its own minimum ratings to show. */
    public boolean isCategoryShown(String category, FeedbackLimits limits) {
        return categoryCount(category) >= limits.minimumRatingsToShow();
    }

    /** How many ratings this summary counts. */
    public int ratingCount() {
        return overall.count;
    }

    public Subject subject() {
        return subject;
    }

    public String subjectId() {
        return subjectId;
    }

    private Tally tallyOf(String category) {
        requireCategory(category);
        return categories.getOrDefault(category, new Tally());
    }

    /** A provider summary is rated only on provider categories, and a helper summary only on helper categories. */
    private void requireCategory(String category) {
        if (!subject.categories().contains(category)) {
            throw new RatingDoesNotFitSummaryException(
                    category + " is not a " + subject.name().toLowerCase() + " category");
        }
    }

    /** Running count and total of stars. */
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

        BigDecimal average() {
            if (count == 0) {
                return BigDecimal.ZERO.setScale(1);
            }
            return BigDecimal.valueOf(total).divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
        }
    }
}
