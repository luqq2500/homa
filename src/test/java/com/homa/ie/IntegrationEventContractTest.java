package com.homa.ie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.AvailabilityChangedIE;
import com.homa.ie.events.ListingTermsChangedIE;
import com.homa.kernel.Money;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IntegrationEventContractTest {

    @Test
    void a_terms_snapshot_cannot_change_after_it_is_published() {
        List<String> cities = new ArrayList<>(HomaFixtures.KEMAS_HOME_CITIES);
        Map<String, Money> byCity = new HashMap<>();
        ListingTermsChangedIE event = new ListingTermsChangedIE(
                HomaFixtures.KEMAS_HOME_PROVIDER_ID, cities, List.of("HOUSE_CLEANING"),
                HomaFixtures.HOURLY_RATE_PER_HELPER, "FLAT", HomaFixtures.FLAT_TRAVEL_FEE, byCity,
                HomaFixtures.DISCOUNT_RATE, HomaFixtures.DISCOUNT_MIN_HOURS, HomaFixtures.DISCOUNT_MIN_HELPERS,
                Duration.ofHours(1), Duration.ofHours(8), 1, 3, HomaFixtures.MINIMUM_NOTICE, HomaFixtures.JOB_BUFFER,
                HomaFixtures.ACCEPTANCE_POLICY, HomaFixtures.CANCELLATION_TIER, false, HomaFixtures.REQUESTED_AT);

        cities.add("PUCHONG");
        byCity.put("PUCHONG", Money.myr(30));

        assertThat(event.cities()).containsExactlyElementsOf(HomaFixtures.KEMAS_HOME_CITIES);
        assertThat(event.travelFeeByCity()).isEmpty();
        assertThatThrownBy(() -> event.jobCodes().add("X")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void weekly_hours_cannot_change_after_they_are_published() {
        List<String> hours = new ArrayList<>(List.of("SATURDAY 08:00-18:00"));
        AvailabilityChangedIE event = new AvailabilityChangedIE(
                HomaFixtures.KEMAS_HOME_PROVIDER_ID, hours, HomaFixtures.REQUESTED_AT);

        hours.add("SUNDAY 08:00-18:00");

        assertThat(event.weeklyHours()).containsExactly("SATURDAY 08:00-18:00");
    }
}
