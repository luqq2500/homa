package offering.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kernel.Money;
import offering.domain.exception.HourlyRateNotPositiveException;
import offering.domain.exception.InvalidHelpersRangeException;
import offering.domain.exception.InvalidHomaLimitsException;
import offering.domain.exception.OfferingDetailRequiredException;
import offering.domain.exception.ProfileNameRequiredException;
import offering.domain.exception.TravelFeeNotPositiveException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** The terms a listing is made of: rate, travel fee, ranges, profile, team and Homa's limits. */
class OfferingTermsTest {

    @Nested
    class Pricing {

        @Test
        void theHourlyRateIsAboveZero() {
            assertThat(HourlyRate.of(Money.ofSen(1)).perHelperPerHour()).isEqualTo(Money.ofSen(1));
            assertThatThrownBy(() -> HourlyRate.of(Money.ZERO)).isInstanceOf(HourlyRateNotPositiveException.class);
            assertThatThrownBy(() -> HourlyRate.of(Money.ofSen(-1))).isInstanceOf(HourlyRateNotPositiveException.class);
        }

        @Test
        void aSeparateTravelFeeIsAboveZero() {
            assertThat(TravelFee.separate(Money.ofSen(1)).separateAmount()).contains(Money.ofSen(1));
            assertThatThrownBy(() -> TravelFee.separate(Money.ZERO)).isInstanceOf(TravelFeeNotPositiveException.class);
            assertThatThrownBy(() -> TravelFee.separate(Money.ofSen(-1))).isInstanceOf(TravelFeeNotPositiveException.class);
        }

        @Test
        void travelIsEitherSeparateOrIncluded() {
            assertThat(TravelFee.included().isIncluded()).isTrue();
            assertThat(TravelFee.included().separateAmount()).isEmpty();
            assertThat(TravelFee.separate(Money.parse("20")).isIncluded()).isFalse();
        }
    }

    @Nested
    class Ranges {

        @ParameterizedTest(name = "{0} hours in 2 to 4 is {1}")
        @CsvSource({"1, false", "2, true", "3, true", "4, true", "5, false"})
        void anHoursRangeIncludesBothEnds(int hours, boolean inside) {
            assertThat(HoursRange.of(2, 4).contains(hours)).isEqualTo(inside);
        }

        @ParameterizedTest(name = "{0} helpers in 1 to 3 is {1}")
        @CsvSource({"0, false", "1, true", "3, true", "4, false"})
        void aHelpersRangeIncludesBothEnds(int helpers, boolean inside) {
            assertThat(HelpersRange.of(1, 3).contains(helpers)).isEqualTo(inside);
        }

        @Test
        void theMinimumHelpersCannotBeAboveTheMaximum() {
            assertThatThrownBy(() -> HelpersRange.of(3, 2)).isInstanceOf(InvalidHelpersRangeException.class);
            assertThat(HelpersRange.of(2, 2).max()).isEqualTo(2);
        }

        @Test
        void homasLimitsAreRangesFromOne() {
            HomaLimits limits = new HomaLimits(1, 6, 1, 10);
            assertThat(limits.maxHours()).isEqualTo(6);
            assertThat(limits.maxHelpers()).isEqualTo(10);

            assertThatThrownBy(() -> new HomaLimits(0, 6, 1, 10)).isInstanceOf(InvalidHomaLimitsException.class);
            assertThatThrownBy(() -> new HomaLimits(7, 6, 1, 10)).isInstanceOf(InvalidHomaLimitsException.class);
            assertThatThrownBy(() -> new HomaLimits(1, 6, 0, 10)).isInstanceOf(InvalidHomaLimitsException.class);
            assertThatThrownBy(() -> new HomaLimits(1, 6, 11, 10))
                    .isInstanceOf(InvalidHomaLimitsException.class)
                    .hasMessageContaining("helpers per search");
            assertThat(new HomaLimits(6, 6, 10, 10).minHours()).isEqualTo(6);
        }
    }

    @Nested
    class Profiles {

        @Test
        void aProfileHasADisplayNameAndAPicture() {
            Profile profile = Profile.of(" Kemas Home Care ", " photos/kemas.jpg ");

            assertThat(profile.displayName()).isEqualTo("Kemas Home Care");
            assertThat(profile.picture()).contains("photos/kemas.jpg");
            assertThat(profile.hasPicture()).isTrue();
        }

        @ParameterizedTest(name = "picture \"{0}\" is no picture")
        @NullSource
        @ValueSource(strings = {"", "  "})
        void aProfileCanWaitForItsPicture(String picture) {
            assertThat(Profile.of("Kemas Home Care", picture).hasPicture()).isFalse();
            assertThat(Profile.withoutPicture("Kemas Home Care").hasPicture()).isFalse();
        }

        @ParameterizedTest(name = "\"{0}\" is not a display name")
        @NullSource
        @ValueSource(strings = {"", " "})
        void aProfileNeedsADisplayName(String name) {
            assertThatThrownBy(() -> Profile.withoutPicture(name)).isInstanceOf(ProfileNameRequiredException.class);
        }
    }

    @Nested
    class TeamsAndPlaces {

        @Test
        void aRosterKeepsItsVerifiedHelpers() {
            Set<String> ids = new HashSet<>(Set.of("aina", "ben"));
            TeamRoster roster = TeamRoster.of(ids);
            ids.clear();

            assertThat(roster.size()).isEqualTo(2);
            assertThat(roster.includes("aina")).isTrue();
            assertThat(roster.includes("dina")).isFalse();
            assertThat(TeamRoster.empty().size()).isZero();
        }

        @Test
        void aRosterNeedsRealHelperIds() {
            assertThatThrownBy(() -> TeamRoster.of(Set.of(" "))).isInstanceOf(OfferingDetailRequiredException.class);
        }

        @Test
        void codesAndCitiesAreNamedWithoutSurroundingSpaces() {
            assertThat(JobCode.of(" KITCHEN ")).isEqualTo(JobCode.of("KITCHEN")).hasToString("KITCHEN");
            assertThat(ToolCode.of(" STEAM_MOP ")).isEqualTo(ToolCode.of("STEAM_MOP")).hasToString("STEAM_MOP");
            assertThat(City.of(" Cyberjaya ")).isEqualTo(City.of("Cyberjaya")).hasToString("Cyberjaya");
            assertThatThrownBy(() -> JobCode.of("")).isInstanceOf(OfferingDetailRequiredException.class);
            assertThatThrownBy(() -> ToolCode.of(null)).isInstanceOf(OfferingDetailRequiredException.class);
            assertThatThrownBy(() -> City.of(" ")).isInstanceOf(OfferingDetailRequiredException.class);
        }

        @Test
        void theServiceAreaViewSaysWhichCitiesCanBeCovered() {
            ServiceAreaView area = ServiceAreaView.of(Set.of(City.of("Cyberjaya")));

            assertThat(area.covers(City.of("Cyberjaya"))).isTrue();
            assertThat(area.covers(City.of("Kajang"))).isFalse();
        }
    }
}
