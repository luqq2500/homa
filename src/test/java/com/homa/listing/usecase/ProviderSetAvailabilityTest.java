package com.homa.listing.usecase;

import static com.homa.listing.usecase.ListingTestBed.AZMAN;
import static com.homa.listing.usecase.ListingTestBed.HANA;
import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME;
import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME_ID;
import static com.homa.listing.usecase.ListingTestBed.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.AvailabilityChangedIE;
import com.homa.listing.domain.WeeklyHours;
import com.homa.listing.domain.WorkingHours;
import com.homa.listing.domain.exception.InvalidWorkingHoursException;
import com.homa.listing.usecase.dto.ProviderSetAvailabilityRequest;
import com.homa.listing.usecase.dto.ProviderSetAvailabilityResponse;
import com.homa.listing.usecase.exception.InvalidWeeklyHoursException;
import com.homa.listing.usecase.exception.NotAllowedException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("OFR-8")
class ProviderSetAvailabilityTest {

    private static final List<String> MONDAY_TO_SATURDAY_EIGHT_TO_SIX = List.of(
            "SATURDAY 08:00-18:00", "MONDAY 08:00-18:00", "TUESDAY 08:00-18:00", "WEDNESDAY 08:00-18:00",
            "THURSDAY 08:00-18:00", "FRIDAY 08:00-18:00");

    private final ListingTestBed bed = new ListingTestBed().withKemasHomeListing();

    @Test
    @DisplayName("Kemas Home works Monday–Saturday 8:00–18:00; its hours are stored and published, Monday first")
    void kemasHomeSetsItsWeeklyHours() {
        ProviderSetAvailabilityResponse response = bed.setAvailability.set(
                new ProviderSetAvailabilityRequest(AZMAN, KEMAS_HOME, MONDAY_TO_SATURDAY_EIGHT_TO_SIX));

        List<String> mondayFirst = List.of("MONDAY 08:00-18:00", "TUESDAY 08:00-18:00", "WEDNESDAY 08:00-18:00",
                "THURSDAY 08:00-18:00", "FRIDAY 08:00-18:00", "SATURDAY 08:00-18:00");
        assertThat(response).isEqualTo(new ProviderSetAvailabilityResponse(KEMAS_HOME, mondayFirst));
        assertThat(bed.availability.weeklyHoursOf(KEMAS_HOME_ID)).contains(WeeklyHours.on(
                EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY), new WorkingHours(LocalTime.of(8, 0), LocalTime.of(18, 0))));
        AvailabilityChangedIE event = bed.publisher.single(AvailabilityChangedIE.class);
        assertThat(event.eventId()).isEqualTo("listing-event-2");
        assertThat(event.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(event.weeklyHours()).isEqualTo(mondayFirst);
        assertThat(event.occurredAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("New hours replace the previous ones: Saturday 9:30–13:15 only")
    void newHoursReplaceThePreviousOnes() {
        bed.setAvailability.set(new ProviderSetAvailabilityRequest(AZMAN, KEMAS_HOME, MONDAY_TO_SATURDAY_EIGHT_TO_SIX));
        bed.publisher.clear();

        bed.setAvailability.set(new ProviderSetAvailabilityRequest(AZMAN, KEMAS_HOME, List.of("SATURDAY 09:30-13:15")));

        assertThat(bed.availability.weeklyHoursOf(KEMAS_HOME_ID)).contains(WeeklyHours.on(
                EnumSet.of(DayOfWeek.SATURDAY), new WorkingHours(LocalTime.of(9, 30), LocalTime.of(13, 15))));
        assertThat(bed.publisher.single(AvailabilityChangedIE.class).weeklyHours()).containsExactly("SATURDAY 09:30-13:15");
    }

    @Test
    @DisplayName("Hours that aren't \"DAY HH:MM-HH:MM\" are refused")
    void malformedHoursAreRefused() {
        for (String malformed : List.of("Saturday 08:00-18:00", "SATURDAY 8:00-18:00", "SATURDAY 08:00-24:00",
                "SATURDAY 08:00-18:60", "SATURDAY 08:00 18:00", " SATURDAY 08:00-18:00", "SATURDAY 08:00-18:00 ")) {
            assertThatThrownBy(() -> bed.setAvailability.set(
                    new ProviderSetAvailabilityRequest(AZMAN, KEMAS_HOME, List.of(malformed))))
                    .as(malformed)
                    .isInstanceOf(InvalidWeeklyHoursException.class)
                    .hasMessageContaining(malformed);
        }
        assertThat(bed.availability.weeklyHoursOf(KEMAS_HOME_ID)).isEmpty();
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("One entry per working day: Saturday twice is refused")
    void aDayGivenTwiceIsRefused() {
        assertThatThrownBy(() -> bed.setAvailability.set(new ProviderSetAvailabilityRequest(AZMAN, KEMAS_HOME,
                List.of("SATURDAY 08:00-12:00", "SATURDAY 13:00-18:00"))))
                .isInstanceOf(InvalidWeeklyHoursException.class)
                .hasMessageContaining("SATURDAY");
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("Working hours that end before they start are refused")
    void hoursEndingBeforeTheyStartAreRefused() {
        assertThatThrownBy(() -> bed.setAvailability.set(
                new ProviderSetAvailabilityRequest(AZMAN, KEMAS_HOME, List.of("SUNDAY 18:00-08:00"))))
                .isInstanceOf(InvalidWorkingHoursException.class);
    }

    @Test
    @DisplayName("Only Kemas Home's owner sets its hours")
    void onlyTheOwnerSetsHours() {
        assertThatThrownBy(() -> bed.setAvailability.set(
                new ProviderSetAvailabilityRequest(HANA, KEMAS_HOME, MONDAY_TO_SATURDAY_EIGHT_TO_SIX)))
                .isInstanceOf(NotAllowedException.class);
        assertThat(bed.availability.weeklyHoursOf(KEMAS_HOME_ID)).isEmpty();
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("No working days is allowed: the provider can't be booked at any time")
    void noWorkingDays() {
        bed.setAvailability.set(new ProviderSetAvailabilityRequest(AZMAN, KEMAS_HOME, List.of()));

        assertThat(bed.publisher.single(AvailabilityChangedIE.class).weeklyHours()).isEmpty();
    }
}
