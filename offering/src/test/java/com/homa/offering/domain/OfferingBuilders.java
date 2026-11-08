package com.homa.offering.domain;

import com.homa.kernel.Money;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Test-data builders for offerings, read as domain sentences. */
final class OfferingBuilders {

    static final HomaLimits HOMA_LIMITS = new HomaLimits(1, 6, 1, 10);
    static final Set<JobCode> CATALOGUE_JOBS = Set.of(job("KITCHEN"), job("LAUNDRY_FOLD"), job("TOILET_CARE"));
    static final Set<ToolCode> CATALOGUE_TOOLS = Set.of(tool("STEAM_MOP"), tool("VACUUM"));
    static final ServiceAreaView LAUNCH_AREA =
            ServiceAreaView.of(Set.of(City.of("Salak Tinggi"), City.of("Cyberjaya"), City.of("Putrajaya")));
    static final Profile A_PROFILE = Profile.of("Kemas Home Care", "photos/kemas.jpg");

    private OfferingBuilders() {
    }

    static JobCode job(String code) {
        return JobCode.of(code);
    }

    static ToolCode tool(String code) {
        return ToolCode.of(code);
    }

    static City city(String name) {
        return City.of(name);
    }

    /** A verified team of {@code size} helpers, with ids helper-1, helper-2, ... */
    static TeamRoster aRosterOf(int size) {
        return TeamRoster.of(IntStream.rangeClosed(1, size).mapToObj(i -> "helper-" + i).collect(Collectors.toSet()));
    }

    static ProviderListing aDraftWithATeamOf(int size) {
        ProviderListing listing = ProviderListing.draft("provider-1");
        listing.updateRoster(aRosterOf(size));
        return listing;
    }

    /** A listing with a profile, a job, pricing, hours, helpers 1 to 4, Cyberjaya and a tier, not yet live. */
    static ProviderListing aCompleteListing() {
        ProviderListing listing = aDraftWithATeamOf(4);
        listing.setProfile(A_PROFILE);
        listing.offerJobs(Set.of(job("KITCHEN"), job("LAUNDRY_FOLD")), CATALOGUE_JOBS);
        listing.bringTools(Set.of(tool("STEAM_MOP")), CATALOGUE_TOOLS);
        listing.setPricing(HourlyRate.of(Money.parse("30")), TravelFee.separate(Money.parse("20")));
        listing.setHours(HoursRange.of(2, 4), HOMA_LIMITS);
        listing.setHelpers(HelpersRange.of(1, 4), HOMA_LIMITS);
        listing.coverCities(Set.of(city("Cyberjaya")), LAUNCH_AREA);
        listing.chooseCancellationTier(CancellationTier.STANDARD);
        return listing;
    }

    static ProviderListing aLiveListing() {
        ProviderListing listing = aCompleteListing();
        listing.goLive(true, true);
        return listing;
    }
}
