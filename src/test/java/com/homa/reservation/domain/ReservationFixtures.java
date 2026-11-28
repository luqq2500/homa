package com.homa.reservation.domain;

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
import static com.homa.fixtures.HomaFixtures.MINIMUM_NOTICE;
import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.RINA_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.STANDARD_HELPERS;
import static com.homa.fixtures.HomaFixtures.STANDARD_HOURS;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.fixtures.HomaFixtures;
import com.homa.kernel.Money;
import com.homa.kernel.TimeInterval;
import com.homa.reservation.domain.exception.BookingRefusedException;
import com.homa.reservation.domain.replica.BookableProvider;
import com.homa.reservation.domain.replica.BookingBounds;
import com.homa.reservation.domain.replica.CustomerStanding;
import com.homa.reservation.domain.replica.DailyHours;
import com.homa.reservation.domain.replica.OfferedTerms;
import com.homa.reservation.domain.replica.RosterableMember;
import com.homa.reservation.domain.replica.TravelFeeRule;
import com.homa.reservation.domain.replica.WorkingHours;
import com.homa.reservation.domain.roster.MemberRole;
import com.homa.reservation.domain.schedule.CustomerSchedule;
import com.homa.reservation.domain.schedule.ProviderSchedule;
import com.homa.reservation.domain.terms.AcceptancePolicy;
import com.homa.reservation.domain.terms.CancellationTier;
import com.homa.reservation.domain.terms.DiscountRule;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/**
 * The §0 example setup as Reservation sees it, built from {@link HomaFixtures}: Aisyah, Kemas Home and the
 * standard reservation, Saturday 5 December 2026 10:00–14:00 with 2 helpers, made on Monday 30 November at 20:00.
 */
final class ReservationFixtures {

    private ReservationFixtures() {
    }

    static final String KITCHEN_CLEANING = "KITCHEN_CLEANING";
    static final String LAUNDRY_FOLDING = "LAUNDRY_FOLDING";
    static final String BATHROOM_CLEANING = "BATHROOM_CLEANING";
    static final LocalDate SATURDAY = LocalDate.of(2026, 12, 5);
    static final String ANOTHER_CUSTOMER_ID = "customer-another";
    static final String ANOTHER_PROVIDER_ID = "provider-another";
    static final String MEI_CUSTOMER_ID = "customer-mei";
    /** Azman, Kemas Home's owner; Siti, Nurul, Rina and Farid are the people its members are (PRV-11). */
    static final String AZMAN_PERSON_ID = "person-azman";
    static final String SITI_PERSON_ID = "person-siti";
    static final String NURUL_PERSON_ID = "person-nurul";
    static final String RINA_PERSON_ID = "person-rina";
    static final String FARID_PERSON_ID = "person-farid";
    /** Someone who is neither Kemas Home's owner nor on its Saturday roster. */
    static final String STRANGER_PERSON_ID = "person-stranger";
    /** Listing's booking step: durations and start times move in 15 minutes (RES-5, OFR-5). */
    static final Duration BOOKING_STEP = Duration.ofMinutes(15);

    /** 19:58 on Monday: Aisyah taps Pay for the standard reservation. Nothing is held (RES-11). */
    static final Instant TAPPED_PAY_AT = REQUESTED_AT.minus(Duration.ofMinutes(2));
    /** 22:00 on Monday: Kemas Home accepts (ACC-2). */
    static final Instant ACCEPTED_AT = monday(22, 0);

    static Instant monday(int hour, int minute) {
        return HomaFixtures.at(2026, 11, 30, hour, minute);
    }

    static Instant tuesday(int hour, int minute) {
        return HomaFixtures.at(2026, 12, 1, hour, minute);
    }

    static Instant friday(int hour, int minute) {
        return HomaFixtures.at(2026, 12, 4, hour, minute);
    }

    static Instant saturday(int hour, int minute) {
        return HomaFixtures.at(2026, 12, 5, hour, minute);
    }

    static Instant sunday(int hour, int minute) {
        return HomaFixtures.at(2026, 12, 6, hour, minute);
    }

    // Customers

    static ServiceAddress aisyahAddress() {
        return new ServiceAddress("B-12-3, Jalan Salak Tinggi", AISYAH_CITY, "Guardhouse: say unit B-12-3");
    }

    /** §0: Aisyah is identity verified (CUS-3). */
    static CustomerStanding aisyah() {
        return CustomerStanding.registered(AISYAH_CUSTOMER_ID, aisyahAddress()).verified();
    }

    static ServiceAddress anotherCustomerAddress() {
        return new ServiceAddress("7, Persiaran Cyberjaya", "CYBERJAYA", "Lobby lift to level 3");
    }

    static CustomerStanding anotherCustomer() {
        return CustomerStanding.registered(ANOTHER_CUSTOMER_ID, anotherCustomerAddress()).verified();
    }

    static ServiceAddress meiAddress() {
        return new ServiceAddress("3A, Presint 9, Putrajaya", "PUTRAJAYA", "Blue gate");
    }

    /** RES-11 case: Mei pays for the same Saturday time as Aisyah. */
    static CustomerStanding mei() {
        return CustomerStanding.registered(MEI_CUSTOMER_ID, meiAddress()).verified();
    }

    // Kemas Home

    /** Monday to Saturday, 8:00–18:00 (OFR-8). */
    static WorkingHours kemasWorkingHours() {
        Map<DayOfWeek, DailyHours> days = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)) {
            days.put(day, new DailyHours(LocalTime.of(8, 0), LocalTime.of(18, 0)));
        }
        return new WorkingHours(days);
    }

    static ProviderBuilder kemasHomeProvider() {
        return new ProviderBuilder();
    }

    static BookableProvider kemasHome() {
        return kemasHomeProvider().build();
    }

    /** Kemas Home's settings, changeable one at a time. */
    static final class ProviderBuilder {
        private String providerId = KEMAS_HOME_PROVIDER_ID;
        private String ownerPersonId = AZMAN_PERSON_ID;
        private boolean bookable = true;
        private Set<String> cities = new HashSet<>(KEMAS_HOME_CITIES);
        private Set<String> jobs = Set.of(KITCHEN_CLEANING, LAUNDRY_FOLDING, BATHROOM_CLEANING);
        private Money rate = HOURLY_RATE_PER_HELPER;
        private TravelFeeRule travelFee = TravelFeeRule.flat(FLAT_TRAVEL_FEE);
        private DiscountRule discount = new DiscountRule(DISCOUNT_RATE, DISCOUNT_MIN_HOURS, DISCOUNT_MIN_HELPERS);
        private AcceptancePolicy policy = AcceptancePolicy.valueOf(HomaFixtures.ACCEPTANCE_POLICY);
        private CancellationTier tier = CancellationTier.valueOf(HomaFixtures.CANCELLATION_TIER);
        /** OFR-5 case: Kemas Home sets 2 to 6 hours and leaves helpers on default. */
        private BookingBounds bounds =
                new BookingBounds(Duration.ofHours(2), Duration.ofHours(6), BOOKING_STEP, 1, Integer.MAX_VALUE);
        /** Siti, Nurul and Rina. */
        private int activeHelpers = 3;
        private Duration notice = MINIMUM_NOTICE;
        private Duration buffer = JOB_BUFFER;
        private WorkingHours hours = kemasWorkingHours();
        private List<TimeInterval> busyTimes = new ArrayList<>();

        ProviderBuilder id(String value) {
            this.providerId = value;
            return this;
        }

        ProviderBuilder notBookable() {
            this.bookable = false;
            return this;
        }

        ProviderBuilder cities(String... values) {
            this.cities = Set.of(values);
            return this;
        }

        ProviderBuilder rate(Money value) {
            this.rate = value;
            return this;
        }

        ProviderBuilder travelFee(TravelFeeRule value) {
            this.travelFee = value;
            return this;
        }

        ProviderBuilder discount(DiscountRule value) {
            this.discount = value;
            return this;
        }

        ProviderBuilder policy(AcceptancePolicy value) {
            this.policy = value;
            return this;
        }

        ProviderBuilder tier(CancellationTier value) {
            this.tier = value;
            return this;
        }

        ProviderBuilder bounds(BookingBounds value) {
            this.bounds = value;
            return this;
        }

        ProviderBuilder activeHelpers(int value) {
            this.activeHelpers = value;
            return this;
        }

        ProviderBuilder minimumNotice(Duration value) {
            this.notice = value;
            return this;
        }

        ProviderBuilder buffer(Duration value) {
            this.buffer = value;
            return this;
        }

        ProviderBuilder workingHours(WorkingHours value) {
            this.hours = value;
            return this;
        }

        ProviderBuilder busy(TimeInterval value) {
            this.busyTimes.add(value);
            return this;
        }

        BookableProvider build() {
            return new BookableProvider(providerId, ownerPersonId, bookable, cities, jobs,
                    new OfferedTerms(rate, travelFee, discount, policy, tier), bounds, activeHelpers, notice, buffer,
                    hours, busyTimes);
        }
    }

    // Bookings

    /** Kitchen cleaning and laundry folding, Saturday 10:00–14:00, 2 helpers, at Aisyah's home. */
    static Booking standardBooking() {
        return aisyahBooking(JOB_START, STANDARD_HOURS, STANDARD_HELPERS);
    }

    static Booking aisyahBooking(Instant start, Duration hours, int helpers) {
        return new Booking(AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID,
                JobSelection.of(KITCHEN_CLEANING, LAUNDRY_FOLDING), JobTime.of(start, hours), helpers, aisyahAddress());
    }

    static Booking anotherCustomerBooking(Instant start, Duration hours, int helpers) {
        return new Booking(ANOTHER_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, JobSelection.of(BATHROOM_CLEANING),
                JobTime.of(start, hours), helpers, anotherCustomerAddress());
    }

    /** RES-11 case: Mei books Kemas Home for the same Saturday 10:00–14:00 with 2 helpers. */
    static Booking meiBooking() {
        return new Booking(MEI_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, JobSelection.of(KITCHEN_CLEANING),
                JobTime.of(JOB_START, STANDARD_HOURS), STANDARD_HELPERS, meiAddress());
    }

    // The team

    static RosterableMember siti() {
        return RosterableMember.local(SITI_MEMBER_ID, SITI_PERSON_ID, KEMAS_HOME_PROVIDER_ID, MemberRole.HELPER, "Siti",
                "photo-siti", true);
    }

    static RosterableMember nurul() {
        return RosterableMember.local(NURUL_MEMBER_ID, NURUL_PERSON_ID, KEMAS_HOME_PROVIDER_ID, MemberRole.HELPER, "Nurul",
                "photo-nurul", true);
    }

    /**
     * ROS-4 case: Rina's permit expires on 20 March 2027, so Provider publishes 20 December 2026 as the last date a
     * job of hers may end on (its 90-day buffer).
     */
    static RosterableMember rina() {
        return RosterableMember.foreign(RINA_MEMBER_ID, RINA_PERSON_ID, KEMAS_HOME_PROVIDER_ID, MemberRole.HELPER, "Rina",
                "photo-rina", true, LocalDate.of(2026, 12, 20));
    }

    static RosterableMember farid() {
        return RosterableMember.local(FARID_MEMBER_ID, FARID_PERSON_ID, KEMAS_HOME_PROVIDER_ID, MemberRole.SUPERVISOR, "Farid",
                "photo-farid", true);
    }

    static RosterableMember deactivated(RosterableMember member) {
        return new RosterableMember(member.memberId(), member.personId(), member.providerId(), member.role(),
                member.name(), member.photoVerified(), member.photoReference(), false, member.rosterableUntil());
    }

    /** The member as Provider announced them on joining, before Reservation heard of a verified photo (IDN-4). */
    static RosterableMember withoutVerifiedPhoto(RosterableMember member) {
        return new RosterableMember(member.memberId(), member.personId(), member.providerId(), member.role(),
                member.name(), false, Optional.empty(), member.active(), member.rosterableUntil());
    }

    // The marketplace around one provider

    /** Kemas Home, its schedule, and the customers and their schedules. */
    static final class Marketplace {
        private BookableProvider provider;
        private final ProviderSchedule providerSchedule;
        private final Map<String, CustomerStanding> customers = new HashMap<>();
        private final Map<String, CustomerSchedule> customerSchedules = new HashMap<>();

        Marketplace(BookableProvider provider) {
            this.provider = provider;
            this.providerSchedule = new ProviderSchedule(provider.providerId());
            register(aisyah());
            register(anotherCustomer());
            register(mei());
        }

        static Marketplace withKemasHome() {
            return new Marketplace(kemasHome());
        }

        Marketplace register(CustomerStanding customer) {
            customers.put(customer.customerId(), customer);
            return this;
        }

        /** The provider changes its settings (a new Listing or Provider event). */
        Marketplace providerBecomes(BookableProvider changed) {
            this.provider = changed;
            return this;
        }

        BookableProvider provider() {
            return provider;
        }

        ProviderSchedule providerSchedule() {
            return providerSchedule;
        }

        CustomerSchedule scheduleOf(String customerId) {
            return customerSchedules.computeIfAbsent(customerId, CustomerSchedule::new);
        }

        Checkout checkout(Booking booking, Instant now) {
            return Checkout.start(CheckoutId.generate(), booking, provider, customers.get(booking.customerId()),
                    providerSchedule, scheduleOf(booking.customerId()), now);
        }

        Reservation pay(Checkout checkout, Instant paidAt) {
            String customerId = checkout.booking().customerId();
            return Reservation.request(ReservationId.generate(), checkout, provider, customers.get(customerId),
                    providerSchedule, scheduleOf(customerId), paidAt);
        }

        Reservation book(Booking booking, Instant checkoutAt, Instant paidAt) {
            return pay(checkout(booking, checkoutAt), paidAt);
        }

        CustomerStanding customer(String customerId) {
            return customers.get(customerId);
        }

        /** The standard reservation: Aisyah taps Pay at 19:58 and her payment is confirmed at 20:00 (RES-7). */
        Reservation requestStandard() {
            return book(standardBooking(), TAPPED_PAY_AT, REQUESTED_AT);
        }

        /** The standard reservation, accepted by Kemas Home on Monday 22:00 (ACC-2). */
        Reservation reserveStandard() {
            Reservation reservation = requestStandard();
            reservation.accept(ACCEPTED_AT);
            return reservation;
        }
    }

    static void assertRefused(ThrowingCallable call, BookingRefusal refusal) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BookingRefusedException.class,
                refused -> org.assertj.core.api.Assertions.assertThat(refused.refusal()).isEqualTo(refusal));
    }
}
