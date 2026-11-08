package feedback.domain;

import feedback.domain.exception.NoRatingToWithdrawException;
import feedback.domain.exception.RatingDoesNotFitSummaryException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What is shown for one provider or one helper: how many ratings, the average stars, and how often each
 * highlight (for a provider) or compliment (for a helper) was picked. Picks are counted, not averaged. A
 * hidden rating is withdrawn so it no longer counts.
 */
public final class RatingSummary {

    /** Whom the summary is about. Providers are picked for highlights; helpers for compliments. */
    public enum Subject {
        PROVIDER(Arrays.stream(Highlight.values()).map(Enum::name).toList()),
        HELPER(Arrays.stream(Compliment.values()).map(Enum::name).toList());

        private final List<String> picks;

        Subject(List<String> picks) {
            this.picks = picks;
        }

        /** The picks this subject can be given, in the order they are listed. */
        public List<String> picks() {
            return picks;
        }
    }

    private final Subject subject;
    private final String subjectId;
    private int count;
    private long starsTotal;
    private final Map<String, Integer> picked = new HashMap<>();

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

    /** Counts one rating: its stars, and each highlight or compliment it picked. */
    public void record(Stars stars, Set<String> picks) {
        requireFits(picks);
        count++;
        starsTotal += stars.value();
        picks.forEach(pick -> picked.merge(pick, 1, Integer::sum));
    }

    /** Takes back a rating that was counted, for example one Homa hid. */
    public void withdraw(Stars stars, Set<String> picks) {
        requireFits(picks);
        if (count == 0 || !picked.keySet().containsAll(picks)) {
            throw new NoRatingToWithdrawException(subject.name().toLowerCase() + " " + subjectId);
        }
        count--;
        starsTotal -= stars.value();
        picks.forEach(pick -> picked.merge(pick, -1, Integer::sum));
        picked.values().removeIf(times -> times == 0);
    }

    /** The average stars, to one decimal place, rounded half-up. Zero before any rating. */
    public BigDecimal averageStars() {
        if (count == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(starsTotal).divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
    }

    /** Whether the average is shown yet: only from the minimum ratings to show. Until then it shows as "New". */
    public boolean isShown(FeedbackLimits limits) {
        return count >= limits.minimumRatingsToShow();
    }

    /** The most-picked highlights or compliments with their counts, most first; ties keep the listed order. */
    public List<PickCount> topPicks(int n) {
        return picked.entrySet().stream()
                .map(entry -> new PickCount(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(PickCount::count).reversed()
                        .thenComparingInt(pick -> subject.picks().indexOf(pick.pick())))
                .limit(n)
                .toList();
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

    /** A provider is picked only for highlights, and a helper only for compliments. */
    private void requireFits(Set<String> picks) {
        picks.stream().filter(pick -> !subject.picks().contains(pick)).findFirst().ifPresent(pick -> {
            throw new RatingDoesNotFitSummaryException(
                    pick + " is not a " + (subject == Subject.PROVIDER ? "highlight" : "compliment"));
        });
    }
}
