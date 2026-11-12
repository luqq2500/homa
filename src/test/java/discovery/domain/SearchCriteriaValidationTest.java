package discovery.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import discovery.domain.exception.DiscoveryDetailRequiredException;
import discovery.domain.exception.DurationOutOfRangeException;
import discovery.domain.exception.HelpersOutOfRangeException;
import discovery.domain.exception.NoJobsChosenException;
import discovery.domain.exception.StartBeyondHorizonException;
import discovery.domain.exception.StartNotOnQuarterHourException;
import discovery.domain.exception.StartTooSoonException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Feature: Search criteria. */
class SearchCriteriaValidationTest {

    static final SearchLimits LAUNCH_LIMITS = new SearchLimits(1, 6, Duration.ofHours(2), Duration.ofDays(30));
    static final Instant SATURDAY_0900 = Instant.parse("2026-11-14T01:00:00Z");
    static final Instant SATURDAY_1200 = Instant.parse("2026-11-14T04:00:00Z");

    private static SearchCriteria search(int hours, int helpers, Instant start) {
        return SearchCriteria.of(Set.of(JobCode.of("KITCHEN")), start, hours, helpers, City.of("Cyberjaya"), SATURDAY_0900, LAUNCH_LIMITS);
    }

    @ParameterizedTest(name = "{0} hours with {1} helpers is accepted")
    @CsvSource({"1, 1", "6, 10", "6, 25"})
    void searchValuesWithinHomasRangesAreAccepted(int hours, int helpers) {
        SearchCriteria criteria = search(hours, helpers, SATURDAY_1200);

        assertThat(criteria.hours()).isEqualTo(hours);
        assertThat(criteria.helpers()).isEqualTo(helpers);
        assertThat(criteria.end()).isEqualTo(SATURDAY_1200.plus(Duration.ofHours(hours)));
    }

    @ParameterizedTest(name = "{0} hours is outside Homa's range")
    @CsvSource({"7", "0"})
    void theDurationMustBeWithinHomasRange(int hours) {
        assertThatThrownBy(() -> search(hours, 1, SATURDAY_1200)).isInstanceOf(DurationOutOfRangeException.class);
    }

    @ParameterizedTest(name = "{0} helpers is refused")
    @CsvSource({"0", "-1"})
    void aSearchIsForAtLeastOneHelper(int helpers) {
        assertThatThrownBy(() -> search(2, helpers, SATURDAY_1200))
                .isInstanceOf(HelpersOutOfRangeException.class)
                .hasMessageContaining("was " + helpers);
    }

    @Test
    void theStartMustRespectTheMinimumNotice() {
        assertThatThrownBy(() -> search(2, 1, SATURDAY_0900.plus(Duration.ofMinutes(90)))).isInstanceOf(StartTooSoonException.class);
        assertThat(search(2, 1, SATURDAY_0900.plus(Duration.ofHours(2))).start()).isEqualTo(SATURDAY_0900.plus(Duration.ofHours(2)));
    }

    @Test
    void theStartMustBeWithinTheBookingHorizon() {
        Instant horizon = SATURDAY_0900.plus(Duration.ofDays(30));

        assertThat(search(2, 1, horizon).start()).isEqualTo(horizon);
        assertThatThrownBy(() -> search(2, 1, horizon.plus(Duration.ofMinutes(15)))).isInstanceOf(StartBeyondHorizonException.class);
    }

    @Test
    void theStartIsOnAQuarterHour() {
        assertThatThrownBy(() -> search(2, 1, SATURDAY_1200.plus(Duration.ofMinutes(10))))
                .isInstanceOf(StartNotOnQuarterHourException.class);
        assertThatThrownBy(() -> search(2, 1, SATURDAY_1200.plusNanos(1))).isInstanceOf(StartNotOnQuarterHourException.class);
    }

    @Test
    void aSearchHasAtLeastOneJob() {
        assertThatThrownBy(() -> SearchCriteria.of(Set.of(), SATURDAY_1200, 2, 1, City.of("Cyberjaya"), SATURDAY_0900, LAUNCH_LIMITS))
                .isInstanceOf(NoJobsChosenException.class);
    }

    @Test
    void codesCitiesAndProvidersAreNamed() {
        assertThatThrownBy(() -> JobCode.of(" ")).isInstanceOf(DiscoveryDetailRequiredException.class);
        assertThatThrownBy(() -> City.of(null)).isInstanceOf(DiscoveryDetailRequiredException.class);
        assertThat(City.of(" Cyberjaya ")).isEqualTo(City.of("Cyberjaya"));
        assertThatThrownBy(() -> new SearchLimits(1, 6, null, Duration.ofDays(30))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SearchLimits(1, 6, Duration.ofHours(2), null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void aSearchKeepsTheStartsTheCustomerCouldBook() {
        SearchCriteria search = SearchCriteria.of(Set.of(JobCode.of("KITCHEN")), SATURDAY_1200, 2, 1, City.of("Cyberjaya"), SATURDAY_0900, LAUNCH_LIMITS);

        assertThat(search.earliestStart()).isEqualTo(SATURDAY_0900.plus(Duration.ofHours(2)));
        assertThat(search.latestStart()).isEqualTo(SATURDAY_0900.plus(Duration.ofDays(30)));
        assertThatThrownBy(() -> new SearchCriteria(Set.of(JobCode.of("KITCHEN")), SATURDAY_1200, 2, 1, City.of("Cyberjaya"), null, SATURDAY_1200))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SearchCriteria(Set.of(JobCode.of("KITCHEN")), SATURDAY_1200, 2, 1, City.of("Cyberjaya"), SATURDAY_0900, null))
                .isInstanceOf(NullPointerException.class);
    }
}
