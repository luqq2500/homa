package com.homa.ie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.AvailabilityChangedIE;
import com.homa.ie.events.ListingTermsChangedIE;
import com.homa.ie.events.ReservationCompletedIE;
import com.homa.kernel.Money;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class IntegrationEventContractTest {

    @Test
    @Tag("RES-8")
    @DisplayName("A published terms snapshot is frozen.")
    void termsSnapshotCannotChangeAfterItIsPublished() {
        List<String> cities = new ArrayList<>(HomaFixtures.KEMAS_HOME_CITIES);
        Map<String, Money> byCity = new HashMap<>();
        ListingTermsChangedIE event = new ListingTermsChangedIE(
                "event-1",
                HomaFixtures.KEMAS_HOME_PROVIDER_ID, cities, List.of("HOUSE_CLEANING"),
                HomaFixtures.HOURLY_RATE_PER_HELPER, "FLAT", HomaFixtures.FLAT_TRAVEL_FEE, byCity,
                HomaFixtures.DISCOUNT_RATE, HomaFixtures.DISCOUNT_MIN_HOURS, HomaFixtures.DISCOUNT_MIN_HELPERS,
                Duration.ofHours(1), Duration.ofHours(8), Duration.ofMinutes(15), 1, 3, HomaFixtures.MINIMUM_NOTICE, HomaFixtures.JOB_BUFFER,
                HomaFixtures.ACCEPTANCE_POLICY, HomaFixtures.CANCELLATION_TIER, false, HomaFixtures.REQUESTED_AT);

        cities.add("PUCHONG");
        byCity.put("PUCHONG", Money.myr(30));

        assertThat(event.cities()).containsExactlyElementsOf(HomaFixtures.KEMAS_HOME_CITIES);
        assertThat(event.travelFeeByCity()).isEmpty();
        assertThatThrownBy(() -> event.jobCodes().add("X")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Published working hours are frozen.")
    void weeklyHoursCannotChangeAfterTheyArePublished() {
        List<String> hours = new ArrayList<>(List.of("SATURDAY 08:00-18:00"));
        AvailabilityChangedIE event = new AvailabilityChangedIE(
                "event-2",
                HomaFixtures.KEMAS_HOME_PROVIDER_ID, hours, HomaFixtures.REQUESTED_AT);

        hours.add("SUNDAY 08:00-18:00");

        assertThat(event.weeklyHours()).containsExactly("SATURDAY 08:00-18:00");
    }

    @Test
    @Tag("JOB-6")
    @DisplayName("Completion photo refs travel with the completed event and cannot change after it is published.")
    void completionPhotoRefsCannotChangeAfterTheyArePublished() {
        List<String> photos = new ArrayList<>(List.of("photo-1"));
        ReservationCompletedIE event = new ReservationCompletedIE("event-3", "reservation-1",
                HomaFixtures.AISYAH_CUSTOMER_ID, HomaFixtures.KEMAS_HOME_PROVIDER_ID, HomaFixtures.TOTAL_RESERVATION_FEE,
                HomaFixtures.PROTECTION_WINDOW.end(), HomaFixtures.JOB_END, photos, HomaFixtures.JOB_END);

        photos.add("photo-2");

        assertThat(event.completionPhotoRefs()).containsExactly("photo-1");
    }
}
