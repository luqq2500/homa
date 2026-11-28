package com.homa.listing.usecase;

import static com.homa.fixtures.HomaFixtures.ACCEPTANCE_POLICY;
import static com.homa.fixtures.HomaFixtures.CANCELLATION_TIER;
import static com.homa.fixtures.HomaFixtures.FARID_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.HOURLY_RATE_PER_HELPER;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_CITIES;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_REGISTRATION_NO;
import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.RINA_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.at;

import com.homa.ie.events.ProviderBookabilityChangedIE;
import com.homa.ie.events.ProviderOpenedIE;
import com.homa.ie.events.ProviderWentLiveIE;
import com.homa.ie.events.TeamMemberActivatedIE;
import com.homa.ie.events.TeamMemberDeactivatedIE;
import com.homa.kernel.Money;
import com.homa.listing.domain.City;
import com.homa.listing.domain.JobType;
import com.homa.listing.domain.Preset;
import com.homa.listing.domain.ProviderId;
import com.homa.listing.domain.Tool;
import com.homa.listing.usecase.dto.ProviderCreateListingRequest;
import com.homa.listing.usecase.ie.HandleProviderBookabilityChangedIE;
import com.homa.listing.usecase.ie.HandleProviderOpenedIE;
import com.homa.listing.usecase.ie.HandleProviderWentLiveIE;
import com.homa.listing.usecase.ie.HandleTeamMemberActivatedIE;
import com.homa.listing.usecase.ie.HandleTeamMemberDeactivatedIE;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Every Listing use case and handler wired to in-memory fakes, with Kemas Home's listing from the example setup
 * (§0): kitchen cleaning, laundry folding and bathroom cleaning with a HEPA vacuum, in Salak Tinggi, Cyberjaya and
 * Putrajaya, at RM25, Request and Standard. Azman is its owner, once Provider says he opened it; Hana runs Hana
 * Cleaning.
 */
final class ListingTestBed {

    static final Instant NOW = at(2026, 11, 23, 9, 0);

    static final String KEMAS_HOME = KEMAS_HOME_PROVIDER_ID;
    static final ProviderId KEMAS_HOME_ID = new ProviderId(KEMAS_HOME);
    static final String HANA_CLEANING = "provider-hana-cleaning";
    static final String AZMAN = "person-azman";
    static final String HANA = "person-hana";
    static final String ADMIN = "person-homa-admin";

    static final String KITCHEN_CLEANING = "KITCHEN_CLEANING";
    static final String LAUNDRY_FOLDING = "LAUNDRY_FOLDING";
    static final String BATHROOM_CLEANING = "BATHROOM_CLEANING";
    static final String WINDOW_CLEANING = "WINDOW_CLEANING";
    static final String HEPA_VACUUM = "HEPA_VACUUM";
    static final String STEAM_MOP = "STEAM_MOP";
    static final String SALAK_TINGGI = KEMAS_HOME_CITIES.get(0);
    static final String CYBERJAYA = KEMAS_HOME_CITIES.get(1);
    static final String PUTRAJAYA = KEMAS_HOME_CITIES.get(2);
    static final String SHAH_ALAM = "SHAH_ALAM";

    static final List<String> KEMAS_HOME_JOBS = List.of(KITCHEN_CLEANING, LAUNDRY_FOLDING, BATHROOM_CLEANING);

    final InMemoryListingRepository listings = new InMemoryListingRepository();
    final InMemoryAvailabilityRepository availability = new InMemoryAvailabilityRepository();
    final InMemoryPresetRepository presets = new InMemoryPresetRepository();
    final InMemoryProviderStandingRepository standings = new InMemoryProviderStandingRepository();
    final InMemoryListingOwners owners = new InMemoryListingOwners();
    final RecordingListingIEPublisher publisher = new RecordingListingIEPublisher();
    final InMemoryProcessedMessageStore processed = new InMemoryProcessedMessageStore();
    final RecordingProviderNotifier notifier = new RecordingProviderNotifier();
    final ListingEvents events = new ListingEvents(new FixedTimeSource(NOW), new SequentialEventIds(), standings);

    final ProviderCreateListing createListing =
            new ProviderCreateListing(listings, presets, owners, publisher, events);
    final ProviderUpdateOfferings updateOfferings =
            new ProviderUpdateOfferings(listings, presets, owners, publisher, events);
    final ProviderSetPricingTerms setPricingTerms = new ProviderSetPricingTerms(listings, owners, publisher, events);
    final ProviderSetBookingRules setBookingRules =
            new ProviderSetBookingRules(listings, owners, publisher, events, notifier);
    final ProviderSetAvailability setAvailability = new ProviderSetAvailability(availability, owners, publisher, events);
    final AdminManagePresets managePresets = new AdminManagePresets(presets);
    final SearchProvidersByCity search = new SearchProvidersByCity(listings, standings);

    final HandleProviderOpenedIE opened = new HandleProviderOpenedIE(processed, owners);
    final HandleProviderWentLiveIE wentLive = new HandleProviderWentLiveIE(processed, standings);
    final HandleProviderBookabilityChangedIE bookabilityChanged =
            new HandleProviderBookabilityChangedIE(processed, standings);
    final HandleTeamMemberActivatedIE memberActivated =
            new HandleTeamMemberActivatedIE(processed, standings, listings, publisher, events);
    final HandleTeamMemberDeactivatedIE memberDeactivated =
            new HandleTeamMemberDeactivatedIE(processed, standings, listings, publisher, events);

    /** Homa's presets: everything Kemas Home picks, plus a job, a tool and a city it doesn't. */
    ListingTestBed withHomaPresets() {
        presets.save(Preset.of(
                codes(List.of(KITCHEN_CLEANING, LAUNDRY_FOLDING, BATHROOM_CLEANING, WINDOW_CLEANING), JobType::new),
                codes(List.of(HEPA_VACUUM, STEAM_MOP), Tool::new),
                codes(List.of(SALAK_TINGGI, CYBERJAYA, PUTRAJAYA, SHAH_ALAM), City::new)));
        return this;
    }

    /** Siti, Nurul and Rina are active helpers; Farid is an active supervisor. */
    ListingTestBed withKemasHomeTeam() {
        memberActivated.handle(helperActivated("team-event-siti", SITI_MEMBER_ID));
        memberActivated.handle(helperActivated("team-event-nurul", NURUL_MEMBER_ID));
        memberActivated.handle(helperActivated("team-event-rina", RINA_MEMBER_ID));
        memberActivated.handle(supervisorActivated("team-event-farid", FARID_MEMBER_ID));
        return this;
    }

    /** Provider has told Listing that Azman opened Kemas Home, so he is its owner. */
    ListingTestBed withKemasHomeOpened() {
        opened.handle(kemasHomeOpened("provider-event-opened"));
        return this;
    }

    /** Azman creates Kemas Home's listing; the events it published are cleared. */
    ListingTestBed withKemasHomeListing() {
        withHomaPresets();
        withKemasHomeOpened();
        createListing.create(kemasHomeCreation());
        publisher.clear();
        return this;
    }

    ListingTestBed withKemasHomeLive() {
        wentLive.handle(kemasHomeWentLive("provider-event-live"));
        return this;
    }

    static ProviderCreateListingRequest kemasHomeCreation() {
        return new ProviderCreateListingRequest(AZMAN, KEMAS_HOME, KEMAS_HOME_JOBS, List.of(HEPA_VACUUM),
                KEMAS_HOME_CITIES, HOURLY_RATE_PER_HELPER, ACCEPTANCE_POLICY, CANCELLATION_TIER);
    }

    static ProviderCreateListingRequest kemasHomeCreation(Money rate, String policy) {
        return new ProviderCreateListingRequest(AZMAN, KEMAS_HOME, KEMAS_HOME_JOBS, List.of(HEPA_VACUUM),
                KEMAS_HOME_CITIES, rate, policy, CANCELLATION_TIER);
    }

    static ProviderOpenedIE kemasHomeOpened(String eventId) {
        return new ProviderOpenedIE(eventId, KEMAS_HOME, AZMAN, NOW);
    }

    static ProviderWentLiveIE kemasHomeWentLive(String eventId) {
        return new ProviderWentLiveIE(eventId, KEMAS_HOME, AZMAN, KEMAS_HOME_REGISTRATION_NO, NOW);
    }

    static ProviderBookabilityChangedIE kemasHomeBookability(String eventId, boolean bookable, String reason) {
        return new ProviderBookabilityChangedIE(eventId, KEMAS_HOME, bookable, reason, NOW);
    }

    static TeamMemberActivatedIE helperActivated(String eventId, String memberId) {
        return memberActivated(eventId, memberId, "HELPER");
    }

    static TeamMemberActivatedIE supervisorActivated(String eventId, String memberId) {
        return memberActivated(eventId, memberId, "SUPERVISOR");
    }

    /** Provider deactivated the member (HLP-5). */
    static TeamMemberDeactivatedIE memberDeactivated(String eventId, String memberId) {
        return memberLeft(eventId, memberId, "DEACTIVATED");
    }

    /** Provider removed the member (HLP-8). */
    static TeamMemberDeactivatedIE memberRemoved(String eventId, String memberId) {
        return memberLeft(eventId, memberId, "REMOVED");
    }

    private static TeamMemberDeactivatedIE memberLeft(String eventId, String memberId, String reason) {
        return new TeamMemberDeactivatedIE(eventId, memberId, "person-" + memberId, KEMAS_HOME, reason, NOW);
    }

    private static TeamMemberActivatedIE memberActivated(String eventId, String memberId, String role) {
        return new TeamMemberActivatedIE(eventId, memberId, "person-" + memberId, KEMAS_HOME, memberId, role, "LOCAL",
                true, null, NOW);
    }

    private static <T> Set<T> codes(List<String> codes, java.util.function.Function<String, T> type) {
        return codes.stream().map(type).collect(Collectors.toSet());
    }
}
