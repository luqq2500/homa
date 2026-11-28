package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CITY;
import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_MIN_HELPERS;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_MIN_HOURS;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_RATE;
import static com.homa.fixtures.HomaFixtures.FARID_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.FLAT_TRAVEL_FEE;
import static com.homa.fixtures.HomaFixtures.HOURLY_RATE_PER_HELPER;
import static com.homa.fixtures.HomaFixtures.JOB_BUFFER;
import static com.homa.fixtures.HomaFixtures.JOB_START;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_CITIES;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_REGISTRATION_NO;
import static com.homa.fixtures.HomaFixtures.MINIMUM_NOTICE;
import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.RINA_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.STANDARD_HELPERS;
import static com.homa.fixtures.HomaFixtures.STANDARD_HOURS;

import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.AvailabilityChangedIE;
import com.homa.ie.events.CustomerAddressChangedIE;
import com.homa.ie.events.CustomerRegisteredIE;
import com.homa.ie.events.CustomerVerifiedIE;
import com.homa.ie.events.ListingTermsChangedIE;
import com.homa.ie.events.PaymentFailedIE;
import com.homa.ie.events.PaymentHeldIE;
import com.homa.ie.events.ProfilePhotoChangedIE;
import com.homa.ie.events.ProviderBookabilityChangedIE;
import com.homa.ie.events.ProviderWentLiveIE;
import com.homa.ie.events.TeamMemberActivatedIE;
import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationId;
import com.homa.reservation.domain.ServiceAddress;
import com.homa.reservation.usecase.dto.AcceptReservationRequest;
import com.homa.reservation.usecase.dto.StartReservationCheckoutRequest;
import com.homa.reservation.usecase.dto.StartReservationCheckoutResponse;
import com.homa.reservation.usecase.ie.HandleAvailabilityChangedIE;
import com.homa.reservation.usecase.ie.HandleCustomerAddressChangedIE;
import com.homa.reservation.usecase.ie.HandleCustomerRegisteredIE;
import com.homa.reservation.usecase.ie.HandleCustomerVerifiedIE;
import com.homa.reservation.usecase.ie.HandleListingTermsChangedIE;
import com.homa.reservation.usecase.ie.HandlePaymentFailedIE;
import com.homa.reservation.usecase.ie.HandlePaymentHeldIE;
import com.homa.reservation.usecase.ie.HandleProfilePhotoChangedIE;
import com.homa.reservation.usecase.ie.HandleProviderBookabilityChangedIE;
import com.homa.reservation.usecase.ie.HandleProviderWentLiveIE;
import com.homa.reservation.usecase.ie.HandleTeamMemberActivatedIE;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The Reservation context wired with in-memory fakes, and the §0 setup told to it the way production tells it: by
 * the events of Listing, Provider, Verification and Customer. Aisyah is registered and verified in Salak Tinggi; Kemas
 * Home is listed (Request, Standard, RM25, RM20 flat, 10% from 3 hours and 2 helpers), works Monday to Saturday
 * 8:00–18:00, is live, and has Siti, Nurul and Rina (foreign, until 20 December) as helpers and Farid as supervisor,
 * each with a verified photo. The clock starts at Monday 30 November 19:58, when Aisyah taps Pay.
 */
public final class ReservationTestBed {

    public static final String KITCHEN_CLEANING = "KITCHEN_CLEANING";
    public static final String LAUNDRY_FOLDING = "LAUNDRY_FOLDING";
    public static final String BATHROOM_CLEANING = "BATHROOM_CLEANING";
    public static final String MEI_CUSTOMER_ID = "customer-mei";
    public static final String UNVERIFIED_CUSTOMER_ID = "customer-aisyahs-husband";
    public static final String SHAH_ALAM_CUSTOMER_ID = "customer-aisyahs-sister";
    public static final String STRANGER_ID = "someone-else";
    public static final String INSTANT_PROVIDER_ID = "provider-hana-cleaning";
    public static final String PUTRAJAYA = "PUTRAJAYA";
    public static final Instant TAPPED_PAY_AT = REQUESTED_AT.minus(Duration.ofMinutes(2));
    public static final Instant ACCEPTED_AT = monday(22, 0);
    public static final String CLEAN_KITCHEN_PHOTO = "photo-clean-kitchen";
    /** Every customer the bed registers lives at this address in their own city, with this arrival note (CUS-1). */
    public static final String HOME_STREET_ADDRESS = "B-12-3, Residensi Salak, Jalan Salak 1";
    public static final String HOME_ARRIVAL_NOTE = "Guardhouse: say unit B-12-3";
    /** Where a customer who moves lives next, and how the team reaches it. */
    public static final String NEW_STREET_ADDRESS = "A-3-1, Residensi Cyber, Persiaran Multimedia";
    public static final String NEW_ARRIVAL_NOTE = "Lobby: ask the concierge for unit A-3-1";

    public final InMemorySchedules schedules = new InMemorySchedules();
    public final InMemoryReservationRepository reservations = new InMemoryReservationRepository(schedules);
    public final InMemoryPorts.Checkouts checkouts = new InMemoryPorts.Checkouts();
    public final InMemoryReplicas.Providers providers = new InMemoryReplicas.Providers();
    public final InMemoryReplicas.Members members = new InMemoryReplicas.Members();
    public final InMemoryReplicas.Photos photos = new InMemoryReplicas.Photos();
    public final InMemoryReplicas.Customers customers = new InMemoryReplicas.Customers();
    public final InMemoryPorts.RecordingPublisher publisher = new InMemoryPorts.RecordingPublisher();
    public final InMemoryPorts.ProcessedMessages processed = new InMemoryPorts.ProcessedMessages();
    public final InMemoryPorts.SettableTime time = new InMemoryPorts.SettableTime(TAPPED_PAY_AT);
    public final InMemoryPorts.SequentialEventIds eventIds = new InMemoryPorts.SequentialEventIds();
    public final InMemoryPorts.PhotoStore photoStore = new InMemoryPorts.PhotoStore();
    public final InMemoryPorts.ReminderLog reminderLog = new InMemoryPorts.ReminderLog();
    public final InMemoryPorts.RecordingNotifier notifier = new InMemoryPorts.RecordingNotifier();

    public final ReservationEvents events = new ReservationEvents(eventIds, time);
    public final BookableProviders bookableProviders = new BookableProviders(providers, members);

    public final FindAvailableStartTimes findAvailableStartTimes =
            new FindAvailableStartTimes(customers, bookableProviders, schedules, time);
    public final QuoteReservation quoteReservation = new QuoteReservation(customers, bookableProviders);
    public final CustomerStartReservationCheckout startCheckout = new CustomerStartReservationCheckout(customers,
            bookableProviders, schedules, schedules, checkouts, publisher, events, time);
    public final ProviderAcceptReservation acceptReservation =
            new ProviderAcceptReservation(reservations, publisher, events, time);
    public final CustomerWithdrawReservation withdrawReservation =
            new CustomerWithdrawReservation(reservations, schedules, schedules, publisher, events, time);
    public final ProviderDeclineReservation declineReservation =
            new ProviderDeclineReservation(reservations, schedules, schedules, publisher, events, time);
    public final ExpireRequest expireRequest =
            new ExpireRequest(reservations, schedules, schedules, publisher, events, time);
    public final ExpireOverdueRequests expireOverdueRequests =
            new ExpireOverdueRequests(reservations, expireRequest, time);
    public final PreviewCustomerCancellation previewCancellation =
            new PreviewCustomerCancellation(reservations, time);
    public final CustomerCancelReservation customerCancel =
            new CustomerCancelReservation(reservations, schedules, schedules, publisher, events, time);
    public final ProviderCancelReservation providerCancel =
            new ProviderCancelReservation(reservations, schedules, schedules, publisher, events, time);
    public final ProviderAssignRoster assignRoster =
            new ProviderAssignRoster(reservations, members, publisher, events, time);
    public final ProviderCompleteReservation completeReservation = new ProviderCompleteReservation(reservations,
            schedules, schedules, photoStore, publisher, events, time);
    public final RemindUncompletedJobs remindUncompletedJobs =
            new RemindUncompletedJobs(reservations, reminderLog, notifier, time);
    public final ViewReservation viewReservation = new ViewReservation(reservations);
    public final ListMyReservations listMyReservations = new ListMyReservations(reservations);

    public final HandlePaymentHeldIE paymentHeld = new HandlePaymentHeldIE(processed, checkouts, customers,
            bookableProviders, schedules, schedules, reservations, publisher, events);
    public final HandlePaymentFailedIE paymentFailed = new HandlePaymentFailedIE(processed);
    public final HandleListingTermsChangedIE listingTermsChanged =
            new HandleListingTermsChangedIE(processed, providers);
    public final HandleAvailabilityChangedIE availabilityChanged =
            new HandleAvailabilityChangedIE(processed, providers);
    public final HandleProviderWentLiveIE providerWentLive = new HandleProviderWentLiveIE(processed, providers);
    public final HandleProviderBookabilityChangedIE bookabilityChanged =
            new HandleProviderBookabilityChangedIE(processed, providers);
    public final HandleTeamMemberActivatedIE teamMemberActivated =
            new HandleTeamMemberActivatedIE(processed, members, photos);
    public final HandleProfilePhotoChangedIE profilePhotoChanged =
            new HandleProfilePhotoChangedIE(processed, photos, members);
    public final HandleCustomerRegisteredIE customerRegistered = new HandleCustomerRegisteredIE(processed, customers);
    public final HandleCustomerVerifiedIE customerVerified = new HandleCustomerVerifiedIE(processed, customers);
    public final HandleCustomerAddressChangedIE customerAddressChanged =
            new HandleCustomerAddressChangedIE(processed, customers);

    private int inbound = 1;

    public static Instant monday(int hour, int minute) {
        return HomaFixtures.at(2026, 11, 30, hour, minute);
    }

    public static Instant tuesday(int hour, int minute) {
        return HomaFixtures.at(2026, 12, 1, hour, minute);
    }

    public static Instant friday(int hour, int minute) {
        return HomaFixtures.at(2026, 12, 4, hour, minute);
    }

    public static Instant saturday(int hour, int minute) {
        return HomaFixtures.at(2026, 12, 5, hour, minute);
    }

    public static Instant sunday(int hour, int minute) {
        return HomaFixtures.at(2026, 12, 6, hour, minute);
    }

    /** The §0 world, with nothing published yet. */
    public static ReservationTestBed withKemasHomeAndAisyah() {
        ReservationTestBed bed = new ReservationTestBed();
        bed.listingTermsChanged.handle(bed.kemasListing().build(bed.nextInboundId()));
        bed.availabilityChanged.handle(bed.kemasHours(KEMAS_HOME_PROVIDER_ID));
        bed.providerWentLive.handle(new ProviderWentLiveIE(bed.nextInboundId(), KEMAS_HOME_PROVIDER_ID,
                "person-azman", KEMAS_HOME_REGISTRATION_NO, TAPPED_PAY_AT));
        bed.activateWithVerifiedPhoto(SITI_MEMBER_ID, "person-siti", "Siti", "HELPER", null);
        bed.activateWithVerifiedPhoto(NURUL_MEMBER_ID, "person-nurul", "Nurul", "HELPER", null);
        bed.activateWithVerifiedPhoto(RINA_MEMBER_ID, "person-rina", "Rina", "HELPER", "2026-12-20");
        bed.activateWithVerifiedPhoto(FARID_MEMBER_ID, "person-farid", "Farid", "SUPERVISOR", null);
        bed.registerVerified(AISYAH_CUSTOMER_ID, AISYAH_CITY);
        bed.registerVerified(MEI_CUSTOMER_ID, PUTRAJAYA);
        bed.customerRegistered.handle(new CustomerRegisteredIE(bed.nextInboundId(), UNVERIFIED_CUSTOMER_ID,
                AISYAH_CITY, HOME_STREET_ADDRESS, HOME_ARRIVAL_NOTE, "ACTIVE", TAPPED_PAY_AT));
        bed.registerVerified(SHAH_ALAM_CUSTOMER_ID, "SHAH_ALAM");
        return bed;
    }

    public String nextInboundId() {
        return "inbound-" + inbound++;
    }

    /** Kemas Home's listing, changeable before it is published. */
    public ListingEvent kemasListing() {
        return new ListingEvent(KEMAS_HOME_PROVIDER_ID);
    }

    public AvailabilityChangedIE kemasHours(String providerId) {
        return new AvailabilityChangedIE(nextInboundId(), providerId, List.of(
                "MONDAY 08:00-18:00", "TUESDAY 08:00-18:00", "WEDNESDAY 08:00-18:00", "THURSDAY 08:00-18:00",
                "FRIDAY 08:00-18:00", "SATURDAY 08:00-18:00"), TAPPED_PAY_AT);
    }

    /** A second provider, Hana Cleaning, Instant and otherwise like Kemas Home, with Hana as its one helper. */
    public void listInstantProvider() {
        ListingEvent hana = new ListingEvent(INSTANT_PROVIDER_ID);
        hana.acceptancePolicy = "INSTANT";
        listingTermsChanged.handle(hana.build(nextInboundId()));
        availabilityChanged.handle(kemasHours(INSTANT_PROVIDER_ID));
        providerWentLive.handle(new ProviderWentLiveIE(nextInboundId(), INSTANT_PROVIDER_ID, "person-hana", null,
                TAPPED_PAY_AT));
        profilePhotoChanged.handle(new ProfilePhotoChangedIE(nextInboundId(), "person-hana", true, "photo-hana",
                TAPPED_PAY_AT));
        teamMemberActivated.handle(new TeamMemberActivatedIE(nextInboundId(), "member-hana", "person-hana",
                INSTANT_PROVIDER_ID, "Hana", "HELPER", "LOCAL", true, null, TAPPED_PAY_AT));
        teamMemberActivated.handle(new TeamMemberActivatedIE(nextInboundId(), "member-hana-2", "person-hana-2",
                INSTANT_PROVIDER_ID, "Hana's cousin", "HELPER", "LOCAL", true, null, TAPPED_PAY_AT));
    }

    public void activateWithVerifiedPhoto(String memberId, String personId, String name, String role,
            String rosterableUntil) {
        profilePhotoChanged.handle(new ProfilePhotoChangedIE(nextInboundId(), personId, true,
                "photo-" + name.toLowerCase(), TAPPED_PAY_AT));
        teamMemberActivated.handle(new TeamMemberActivatedIE(nextInboundId(), memberId, personId,
                KEMAS_HOME_PROVIDER_ID, name, role, rosterableUntil == null ? "LOCAL" : "FOREIGN_WORKER", true,
                rosterableUntil, TAPPED_PAY_AT));
    }

    public void registerVerified(String customerId, String city) {
        customerRegistered.handle(new CustomerRegisteredIE(nextInboundId(), customerId, city, HOME_STREET_ADDRESS,
                HOME_ARRIVAL_NOTE, "ACTIVE", TAPPED_PAY_AT));
        customerVerified.handle(new CustomerVerifiedIE(nextInboundId(), customerId, "person-" + customerId,
                TAPPED_PAY_AT));
    }

    public CustomerAddressChangedIE moved(String customerId, String city) {
        return new CustomerAddressChangedIE(nextInboundId(), customerId, city, NEW_STREET_ADDRESS, NEW_ARRIVAL_NOTE,
                "ACTIVE", time.now());
    }

    /** The bed's home address in {@code city}, as Customer's events tell it. */
    public static ServiceAddress homeIn(String city) {
        return new ServiceAddress(HOME_STREET_ADDRESS, city, HOME_ARRIVAL_NOTE);
    }

    public ProviderBookabilityChangedIE bookability(boolean bookable, String reason) {
        return new ProviderBookabilityChangedIE(nextInboundId(), KEMAS_HOME_PROVIDER_ID, bookable, reason,
                time.now());
    }

    // Flow A steps

    /** Kitchen cleaning and laundry folding, Saturday 10:00–14:00, 2 helpers. */
    public static StartReservationCheckoutRequest standardCheckout(String customerId, String providerId) {
        return new StartReservationCheckoutRequest(customerId, providerId, List.of(KITCHEN_CLEANING, LAUNDRY_FOLDING),
                JOB_START, (int) STANDARD_HOURS.toMinutes(), STANDARD_HELPERS);
    }

    public String startStandardCheckout(String customerId) {
        return startCheckout(customerId, KEMAS_HOME_PROVIDER_ID);
    }

    public String startCheckout(String customerId, String providerId) {
        time.set(TAPPED_PAY_AT);
        StartReservationCheckoutResponse started = startCheckout.start(standardCheckout(customerId, providerId));
        return started.checkoutId();
    }

    public PaymentHeldIE paymentHeld(String checkoutId, Instant paidAt) {
        return new PaymentHeldIE(nextInboundId(), checkoutId, Money.myr(200), paidAt, paidAt);
    }

    public PaymentFailedIE paymentFailed(String checkoutId) {
        return new PaymentFailedIE(nextInboundId(), checkoutId, "CARD_DECLINED", time.now());
    }

    /** Aisyah's payment for the standard checkout is confirmed at Monday 20:00, so it is Requested (RES-7). */
    public Reservation requestStandard() {
        String checkoutId = startStandardCheckout(AISYAH_CUSTOMER_ID);
        time.set(REQUESTED_AT);
        paymentHeld.handle(paymentHeld(checkoutId, REQUESTED_AT));
        return reservations.single();
    }

    /** The standard reservation, accepted by Kemas Home on Monday 22:00 (ACC-2). */
    public Reservation reserveStandard() {
        Reservation requested = requestStandard();
        time.set(ACCEPTED_AT);
        acceptReservation.accept(new AcceptReservationRequest(requested.id().value(), KEMAS_HOME_PROVIDER_ID));
        return reload(requested);
    }

    public Reservation reload(Reservation reservation) {
        return reservations.findById(reservation.id()).orElseThrow();
    }

    public Reservation reload(String reservationId) {
        return reservations.findById(new ReservationId(reservationId)).orElseThrow();
    }

    /** A provider's listing as Listing publishes it; Kemas Home's by default. */
    public static final class ListingEvent {
        private final String providerId;
        public List<String> cities = KEMAS_HOME_CITIES;
        public List<String> jobCodes = List.of(KITCHEN_CLEANING, LAUNDRY_FOLDING, BATHROOM_CLEANING);
        public Money rate = HOURLY_RATE_PER_HELPER;
        public String travelFeeKind = "FLAT";
        public Money flatTravelFee = FLAT_TRAVEL_FEE;
        public Map<String, Money> travelFeeByCity = Map.of();
        public Percentage discountRate = DISCOUNT_RATE;
        public Duration discountMinHours = DISCOUNT_MIN_HOURS;
        public int discountMinHelpers = DISCOUNT_MIN_HELPERS;
        public Duration minHours = Duration.ofHours(2);
        public Duration maxHours = Duration.ofHours(6);
        public Duration step = Duration.ofMinutes(15);
        public int minHelpers = 1;
        public int maxHelpers = 6;
        public Duration notice = MINIMUM_NOTICE;
        public Duration buffer = JOB_BUFFER;
        public String acceptancePolicy = "REQUEST";
        public String cancellationTier = "STANDARD";

        ListingEvent(String providerId) {
            this.providerId = providerId;
        }

        public ListingTermsChangedIE build(String eventId) {
            return new ListingTermsChangedIE(eventId, providerId, cities, jobCodes, rate, travelFeeKind,
                    flatTravelFee, travelFeeByCity, discountRate, discountMinHours, discountMinHelpers, minHours,
                    maxHours, step, minHelpers, maxHelpers, notice, buffer, acceptancePolicy, cancellationTier, false,
                    TAPPED_PAY_AT);
        }
    }
}
