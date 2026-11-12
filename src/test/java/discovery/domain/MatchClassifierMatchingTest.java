package discovery.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import discovery.domain.exception.DiscoveryDetailRequiredException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Feature: Matching. Background: the customer searches for KITCHEN and LAUNDRY_FOLD, Saturday 10:00, 3 hours,
 * 2 helpers, in Cyberjaya.
 */
class MatchClassifierMatchingTest {

    private static final Instant THURSDAY_0900 = Instant.parse("2026-11-12T01:00:00Z");
    private static final Instant SATURDAY_1000 = Instant.parse("2026-11-14T02:00:00Z");
    private static final SearchCriteria SEARCH = SearchCriteria.of(
            jobs("KITCHEN", "LAUNDRY_FOLD"), SATURDAY_1000, 3, 2, City.of("Cyberjaya"), THURSDAY_0900, SearchCriteriaValidationTest.LAUNCH_LIMITS);

    private static Set<JobCode> jobs(String... codes) {
        return Arrays.stream(codes).map(JobCode::of).collect(Collectors.toSet());
    }

    /** In Cyberjaya, offering both jobs, hours 2 to 4, helpers 1 to 3, free Saturday 10:00 to 13:00. */
    private static Candidate aProvider() {
        return new Candidate();
    }

    private static final class Candidate {
        private Set<JobCode> offered = jobs("KITCHEN", "LAUNDRY_FOLD");
        private Set<City> cities = Set.of(City.of("Cyberjaya"));
        private int minHours = 2;
        private int maxHours = 4;
        private int minHelpers = 1;
        private int maxHelpers = 3;
        private Optional<Instant> nearestFreeStart = Optional.of(SATURDAY_1000);
        private Instant verifiedUntil = SATURDAY_1000.plus(Duration.ofDays(60));

        /** The provider's re-verification is due at {@code due}. */
        Candidate verifiedUntil(Instant due) {
            verifiedUntil = due;
            return this;
        }

        Candidate offering(String... codes) {
            offered = jobs(codes);
            return this;
        }

        Candidate in(String city) {
            cities = Set.of(City.of(city));
            return this;
        }

        Candidate withHours(int min, int max) {
            minHours = min;
            maxHours = max;
            return this;
        }

        Candidate withHelpers(int min, int max) {
            minHelpers = min;
            maxHelpers = max;
            return this;
        }

        /** Free from Saturday 11:00 instead of 10:00. */
        Candidate freeOnlyNearTheTime() {
            nearestFreeStart = Optional.of(SATURDAY_1000.plus(Duration.ofHours(1)));
            return this;
        }

        Candidate freeFrom(Instant start) {
            nearestFreeStart = Optional.of(start);
            return this;
        }

        Candidate notFreeAtOrNearTheTime() {
            nearestFreeStart = Optional.empty();
            return this;
        }

        MatchResult classified() {
            return MatchClassifier.classify(
                    SEARCH, new ProviderCandidate("provider-1", offered, minHours, maxHours, minHelpers, maxHelpers, cities, nearestFreeStart,
                            verifiedUntil));
        }
    }

    @Test
    void aProviderThatFitsEverythingIsAnExactMatch() {
        MatchResult result = aProvider().classified();

        assertThat(result.match()).isEqualTo(Match.EXACT);
        assertThat(result.reasons()).isEmpty();
        assertThat(result.providerId()).isEqualTo("provider-1");
    }

    @Test
    void someJobsOnlyIsACloseMatch() {
        MatchResult result = aProvider().offering("KITCHEN").classified();

        assertThat(result.match()).isEqualTo(Match.CLOSE);
        assertThat(result.reasons()).containsExactly(MatchReason.MISSING_JOBS);
    }

    @Test
    void freeNearTheTimeIsACloseMatch() {
        MatchResult result = aProvider().freeOnlyNearTheTime().classified();

        assertThat(result.match()).isEqualTo(Match.CLOSE);
        assertThat(result.reasons()).containsExactly(MatchReason.NEARBY_TIME);
    }

    @Test
    void someJobsAndANearbyTimeIsNoMatch() {
        MatchResult result = aProvider().offering("KITCHEN", "TOILET_CARE").freeOnlyNearTheTime().classified();

        assertThat(result.match()).isEqualTo(Match.NONE);
        assertThat(result.reasons()).containsExactlyInAnyOrder(MatchReason.MISSING_JOBS, MatchReason.NEARBY_TIME);
    }

    /** Thursday 09:00 with 2 hours' notice: the earliest bookable start is Thursday 11:00. */
    private static final Instant THURSDAY_1100 = THURSDAY_0900.plus(Duration.ofHours(2));
    /** With a 30-day horizon, the latest bookable start is 30 days after Thursday 09:00. */
    private static final Instant LATEST_BOOKABLE = THURSDAY_0900.plus(Duration.ofDays(30));
    private static final Duration A_QUARTER_HOUR = Duration.ofMinutes(15);

    private static MatchResult classifiedFor(Instant searchedStart, Instant freeFrom) {
        SearchCriteria search = SearchCriteria.of(
                jobs("KITCHEN", "LAUNDRY_FOLD"), searchedStart, 3, 2, City.of("Cyberjaya"), THURSDAY_0900,
                SearchCriteriaValidationTest.LAUNCH_LIMITS);
        return MatchClassifier.classify(
                search, new ProviderCandidate("provider-1", jobs("KITCHEN", "LAUNDRY_FOLD"), 2, 4, 1, 3, Set.of(City.of("Cyberjaya")),
                        Optional.of(freeFrom), SATURDAY_1000.plus(Duration.ofDays(60))));
    }

    @Test
    void aNearbyStartBeforeTheMinimumNoticeIsNotOffered() {
        MatchResult result = classifiedFor(THURSDAY_1100.plus(Duration.ofHours(1)), THURSDAY_1100.minus(A_QUARTER_HOUR));

        assertThat(result.match()).isEqualTo(Match.NONE);
        assertThat(result.reasons()).containsExactly(MatchReason.NOT_FREE);
    }

    @Test
    void aNearbyStartAtTheMinimumNoticeIsOffered() {
        MatchResult result = classifiedFor(THURSDAY_1100.plus(Duration.ofHours(1)), THURSDAY_1100);

        assertThat(result.match()).isEqualTo(Match.CLOSE);
        assertThat(result.reasons()).containsExactly(MatchReason.NEARBY_TIME);
    }

    @Test
    void aNearbyStartAfterTheBookingHorizonIsNotOffered() {
        MatchResult result = classifiedFor(LATEST_BOOKABLE.minus(A_QUARTER_HOUR), LATEST_BOOKABLE.plus(A_QUARTER_HOUR));

        assertThat(result.match()).isEqualTo(Match.NONE);
        assertThat(result.reasons()).containsExactly(MatchReason.NOT_FREE);
    }

    @Test
    void aNearbyStartAtTheBookingHorizonIsOffered() {
        MatchResult result = classifiedFor(LATEST_BOOKABLE.minus(A_QUARTER_HOUR), LATEST_BOOKABLE);

        assertThat(result.match()).isEqualTo(Match.CLOSE);
        assertThat(result.reasons()).containsExactly(MatchReason.NEARBY_TIME);
    }

    @Test
    void notCoveringTheCityIsNoMatch() {
        MatchResult result = aProvider().in("Putrajaya").classified();

        assertThat(result.match()).isEqualTo(Match.NONE);
        assertThat(result.reasons()).containsExactly(MatchReason.CITY_NOT_COVERED);
    }

    @ParameterizedTest(name = "helpers {0} to {1} for 2 helpers: {2}")
    @CsvSource({"1, 1, NONE", "3, 4, NONE", "2, 2, EXACT", "1, 2, EXACT"})
    void helpersOutsideTheRangeIsNoMatch(int min, int max, Match match) {
        MatchResult result = aProvider().withHelpers(min, max).classified();

        assertThat(result.match()).isEqualTo(match);
        assertThat(result.reasons().contains(MatchReason.HELPERS_OUTSIDE_RANGE)).isEqualTo(match == Match.NONE);
    }

    @ParameterizedTest(name = "hours {0} to {1} for 3 hours: {2}")
    @CsvSource({"1, 2, NONE", "4, 6, NONE", "3, 3, EXACT"})
    void aDurationOutsideTheRangeIsNoMatch(int min, int max, Match match) {
        MatchResult result = aProvider().withHours(min, max).classified();

        assertThat(result.match()).isEqualTo(match);
        assertThat(result.reasons().contains(MatchReason.DURATION_OUTSIDE_RANGE)).isEqualTo(match == Match.NONE);
    }

    @Test
    void offeringNoneOfTheJobsIsNoMatch() {
        MatchResult result = aProvider().offering("TOILET_CARE").classified();

        assertThat(result.match()).isEqualTo(Match.NONE);
        assertThat(result.reasons()).containsExactly(MatchReason.NO_CHOSEN_JOBS);
    }

    @Test
    void notFreeAtOrNearTheTimeIsNoMatch() {
        MatchResult result = aProvider().notFreeAtOrNearTheTime().classified();

        assertThat(result.match()).isEqualTo(Match.NONE);
        assertThat(result.reasons()).containsExactly(MatchReason.NOT_FREE);
    }

    @Test
    void everyReasonForNoMatchIsGiven() {
        MatchResult result = aProvider().in("Putrajaya").withHelpers(3, 4).offering("TOILET_CARE").classified();

        assertThat(result.reasons()).containsExactlyInAnyOrder(
                MatchReason.CITY_NOT_COVERED, MatchReason.HELPERS_OUTSIDE_RANGE, MatchReason.NO_CHOSEN_JOBS);
    }

    @Test
    void aProviderFreeEarlierThanTheChosenTimeIsAlsoACloseMatch() {
        MatchResult result = aProvider().freeFrom(SATURDAY_1000.minus(Duration.ofMinutes(15))).classified();

        assertThat(result.match()).isEqualTo(Match.CLOSE);
        assertThat(result.reasons()).containsExactly(MatchReason.NEARBY_TIME);
    }

    @Test
    void aCandidateNeedsAnAvailabilityAnswer() {
        assertThatThrownBy(() -> new ProviderCandidate("provider-1", Set.of(), 1, 1, 1, 1, Set.of(), null, SATURDAY_1000))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProviderCandidate("provider-1", Set.of(), 1, 1, 1, 1, Set.of(), Optional.empty(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void aJobEndingAfterTheProvidersDueDateIsNoMatch() {
        MatchResult result = aProvider().verifiedUntil(SATURDAY_1000.plus(Duration.ofHours(3)).minusSeconds(1)).classified();

        assertThat(result.match()).isEqualTo(Match.NONE);
        assertThat(result.reasons()).containsExactly(MatchReason.VERIFICATION_DUE_BEFORE_END);
    }

    @Test
    void aJobEndingExactlyAtTheDueDateIsAMatch() {
        MatchResult result = aProvider().verifiedUntil(SATURDAY_1000.plus(Duration.ofHours(3))).classified();

        assertThat(result.match()).isEqualTo(Match.EXACT);
    }

    @Test
    void aNearbyStartIsCheckedAgainstTheDueDateToo() {
        Instant dueAtTheChosenEnd = SATURDAY_1000.plus(Duration.ofHours(3));

        MatchResult result = aProvider().freeOnlyNearTheTime().verifiedUntil(dueAtTheChosenEnd).classified();

        assertThat(result.match()).isEqualTo(Match.NONE);
        assertThat(result.reasons()).containsExactly(MatchReason.VERIFICATION_DUE_BEFORE_END);
    }

    @Test
    void aProviderWithNoFreeTimeIsNotFreeRatherThanDue() {
        MatchResult result = aProvider().notFreeAtOrNearTheTime().verifiedUntil(SATURDAY_1000).classified();

        assertThat(result.reasons()).containsExactly(MatchReason.NOT_FREE);
    }

    @Test
    void nearMeansWithinTwoHours() {
        assertThat(MatchClassifier.NEAR_THE_CHOSEN_TIME).isEqualTo(Duration.ofHours(2));
    }

    @Test
    void aCandidateAndAResultBelongToAProvider() {
        assertThatThrownBy(() -> new ProviderCandidate(" ", Set.of(), 1, 1, 1, 1, Set.of(), Optional.empty(), SATURDAY_1000))
                .isInstanceOf(DiscoveryDetailRequiredException.class);
        assertThatThrownBy(() -> new MatchResult(null, Match.EXACT, Set.of())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new MatchResult("provider-1", null, Set.of())).isInstanceOf(NullPointerException.class);
    }
}
