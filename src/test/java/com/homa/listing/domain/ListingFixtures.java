package com.homa.listing.domain;

import static com.homa.fixtures.HomaFixtures.ACCEPTANCE_POLICY;
import static com.homa.fixtures.HomaFixtures.CANCELLATION_TIER;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_MIN_HELPERS;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_MIN_HOURS;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_RATE;
import static com.homa.fixtures.HomaFixtures.FLAT_TRAVEL_FEE;
import static com.homa.fixtures.HomaFixtures.HOURLY_RATE_PER_HELPER;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_CITIES;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Kemas Home's listing from the example setup (§0), built from {@link com.homa.fixtures.HomaFixtures}: kitchen
 * cleaning, laundry folding and bathroom cleaning with a HEPA vacuum in Salak Tinggi, Cyberjaya and Putrajaya; RM25
 * per helper per hour, RM20 flat travel fee, 10% off from 3 hours and 2 helpers, 2 hours' notice, Request, Standard,
 * 30 minutes' buffer; working Monday to Saturday, 8:00–18:00.
 */
final class ListingFixtures {

    private ListingFixtures() {
    }

    static final ProviderId KEMAS_HOME = new ProviderId(KEMAS_HOME_PROVIDER_ID);

    static final JobType KITCHEN_CLEANING = new JobType("KITCHEN_CLEANING");
    static final JobType LAUNDRY_FOLDING = new JobType("LAUNDRY_FOLDING");
    static final JobType BATHROOM_CLEANING = new JobType("BATHROOM_CLEANING");
    static final JobType WINDOW_CLEANING = new JobType("WINDOW_CLEANING");
    static final Tool HEPA_VACUUM = new Tool("HEPA_VACUUM");
    static final Tool STEAM_MOP = new Tool("STEAM_MOP");

    static final City SALAK_TINGGI = new City(KEMAS_HOME_CITIES.get(0));
    static final City CYBERJAYA = new City(KEMAS_HOME_CITIES.get(1));
    static final City PUTRAJAYA = new City(KEMAS_HOME_CITIES.get(2));
    static final City SHAH_ALAM = new City("SHAH_ALAM");

    static final HourlyRate KEMAS_HOME_RATE = new HourlyRate(HOURLY_RATE_PER_HELPER);
    static final TravelFeePolicy KEMAS_HOME_TRAVEL_FEE = TravelFeePolicy.flat(FLAT_TRAVEL_FEE);
    static final DiscountRule KEMAS_HOME_DISCOUNT = DiscountRule.of(DISCOUNT_RATE, DISCOUNT_MIN_HOURS, DISCOUNT_MIN_HELPERS);
    static final AcceptancePolicy KEMAS_HOME_POLICY = AcceptancePolicy.valueOf(ACCEPTANCE_POLICY);
    static final CancellationTier KEMAS_HOME_TIER = CancellationTier.valueOf(CANCELLATION_TIER);

    static final WorkingHours EIGHT_TO_SIX = new WorkingHours(LocalTime.of(8, 0), LocalTime.of(18, 0));
    static final Set<DayOfWeek> MONDAY_TO_SATURDAY = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY);

    /** Homa's presets: everything Kemas Home picks, plus a job, a tool and a city it doesn't. */
    static Preset homaPresets() {
        return Preset.of(
                Set.of(KITCHEN_CLEANING, LAUNDRY_FOLDING, BATHROOM_CLEANING, WINDOW_CLEANING),
                Set.of(HEPA_VACUUM, STEAM_MOP),
                Set.of(SALAK_TINGGI, CYBERJAYA, PUTRAJAYA, SHAH_ALAM));
    }

    static Set<City> kemasHomeCities() {
        return KEMAS_HOME_CITIES.stream().map(City::new).collect(Collectors.toUnmodifiableSet());
    }

    static Offerings kemasHomeOfferings() {
        return new Offerings(Set.of(KITCHEN_CLEANING, LAUNDRY_FOLDING, BATHROOM_CLEANING), Set.of(HEPA_VACUUM),
                kemasHomeCities());
    }

    /** Kemas Home's listing as §0 sets it up. */
    static ProviderListing kemasHomeListing() {
        ProviderListing listing = ProviderListing.create(KEMAS_HOME, kemasHomeOfferings(), homaPresets(), KEMAS_HOME_RATE,
                KEMAS_HOME_POLICY, KEMAS_HOME_TIER);
        listing.setTravelFee(KEMAS_HOME_TRAVEL_FEE);
        listing.setDiscount(KEMAS_HOME_DISCOUNT);
        return listing;
    }

    /** The same listing, but with the Instant policy. */
    static ProviderListing instantListing() {
        return ProviderListing.create(KEMAS_HOME, kemasHomeOfferings(), homaPresets(), KEMAS_HOME_RATE,
                AcceptancePolicy.INSTANT, KEMAS_HOME_TIER);
    }

    static WeeklyHours kemasHomeWeeklyHours() {
        return WeeklyHours.on(MONDAY_TO_SATURDAY, EIGHT_TO_SIX);
    }

    static AvailabilitySchedule kemasHomeAvailability() {
        return AvailabilitySchedule.create(KEMAS_HOME, kemasHomeWeeklyHours());
    }
}
