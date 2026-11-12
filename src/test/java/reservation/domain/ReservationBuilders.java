package reservation.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import kernel.Money;
import kernel.Percentage;

/** Test-data builders for enquiries and reservations, read as domain sentences. */
final class ReservationBuilders {

    static final ZoneId MALAYSIA = ZoneId.of("Asia/Kuala_Lumpur");
    static final LocalDate THURSDAY = LocalDate.of(2026, 11, 12);

    private ReservationBuilders() {
    }

    static Instant thursday(String time) {
        return on(THURSDAY, time);
    }

    static Instant friday(String time) {
        return on(THURSDAY.plusDays(1), time);
    }

    static Instant saturday(String time) {
        return on(THURSDAY.plusDays(2), time);
    }

    static Instant sunday(String time) {
        return on(THURSDAY.plusDays(3), time);
    }

    static Instant nextSaturday(String time) {
        return on(THURSDAY.plusDays(9), time);
    }

    private static Instant on(LocalDate date, String time) {
        return date.atTime(LocalTime.parse(time)).atZone(MALAYSIA).toInstant();
    }

    static Money rm(String amount) {
        return Money.parse(amount);
    }

    static JobCode job(String code) {
        return JobCode.of(code);
    }

    static Set<JobCode> jobs(String... codes) {
        return Arrays.stream(codes).map(JobCode::of).collect(Collectors.toSet());
    }

    static Address anAddressIn(String city) {
        return Address.of("12 Jalan Teknokrat 3", City.of(city));
    }

    /**
     * Homa's values as the reservation features use them: the launch values of version 1, except that the
     * dispute window extension is kept at 1 hour as a test value (the launch value is 24 hours), so the
     * worked times in the features stay readable.
     */
    static PolicySnapshot theLaunchPolicy() {
        return theLaunchPolicyWithHours(1, 6);
    }

    /** Homa's launch values, with a duration range of {@code minHours} to {@code maxHours}. */
    static PolicySnapshot theLaunchPolicyWithHours(int minHours, int maxHours) {
        return new PolicySnapshot(
                1,
                Percentage.of(15),
                Duration.ofHours(2),
                Duration.ofDays(30),
                Duration.ofHours(24),
                Duration.ofHours(24),
                Map.of(
                        CancellationTier.FLEXIBLE, Percentage.ZERO,
                        CancellationTier.STANDARD, Percentage.of(10),
                        CancellationTier.FIRM, Percentage.of(20)),
                Percentage.of(20),
                Percentage.of(30),
                Duration.ofMinutes(15),
                Duration.ofMinutes(15),
                300,
                Duration.ofMinutes(15),
                Percentage.of(30),
                Duration.ofHours(1),
                Duration.ofMinutes(30),
                Duration.ofHours(24),
                Duration.ofHours(1),
                Duration.ofHours(6),
                minHours,
                maxHours,
                Duration.ofHours(1));
    }

    static final EvidenceRef A_PHOTO = EvidenceRef.of("evidence/kitchen-after.jpg");

    /** The provider's team: Aina, Ben and Chan as helpers, and Siti as a supervisor. */
    static final TeamRoster ROSTER = TeamRoster.ofHelpers("aina", "ben", "chan").withSupervisors("siti");

    /** The provider's re-verification due date in most features: well after every booking. */
    static final Instant VERIFIED_UNTIL = Instant.parse("2027-02-01T00:00:00Z");

    static OfferingBuilder anOffering() {
        return new OfferingBuilder();
    }

    static DetailsBuilder aBooking() {
        return new DetailsBuilder();
    }

    static ReservationBuilder aReservation() {
        return new ReservationBuilder();
    }

    /** A live provider in Cyberjaya: RM30 per helper per hour, separate travel RM20, hours 2 to 4, helpers 1 to 3. */
    static final class OfferingBuilder {
        private Money rate = rm("30");
        private Optional<Money> travel = Optional.of(rm("20"));
        private CancellationTier tier = CancellationTier.STANDARD;
        private Set<City> cities = Set.of(City.of("Cyberjaya"));
        private boolean takesRequests = true;
        private Optional<SupervisorRule> supervisorRule = Optional.empty();
        private int minHours = 2;
        private int maxHours = 4;
        private AcceptancePolicy acceptance = AcceptancePolicy.REQUEST_AND_ACCEPT;

        /** The provider uses Instant book: a request is accepted once it is paid. */
        OfferingBuilder withInstantBook() {
            acceptance = AcceptancePolicy.INSTANT_BOOK;
            return this;
        }

        OfferingBuilder withHours(int min, int max) {
            minHours = min;
            maxHours = max;
            return this;
        }

        /** The provider sends a supervisor from {@code helpers} helpers or from {@code hours} hours. */
        OfferingBuilder sendingASupervisorFrom(int helpers, int hours) {
            supervisorRule = Optional.of(new SupervisorRule(OptionalInt.of(helpers), OptionalInt.of(hours)));
            return this;
        }

        /** The listing is paused because the provider's re-verification lapsed. */
        OfferingBuilder paused() {
            takesRequests = false;
            return this;
        }

        OfferingBuilder charging(String ratePerHelperPerHour) {
            rate = rm(ratePerHelperPerHour);
            return this;
        }

        OfferingBuilder withTravelIncluded() {
            travel = Optional.empty();
            return this;
        }

        OfferingBuilder withSeparateTravelFee(String fee) {
            travel = Optional.of(rm(fee));
            return this;
        }

        OfferingBuilder onTier(CancellationTier chosen) {
            tier = chosen;
            return this;
        }

        OfferingBuilder covering(String... cityNames) {
            cities = Arrays.stream(cityNames).map(City::of).collect(Collectors.toSet());
            return this;
        }

        OfferingSnapshot build() {
            return new OfferingSnapshot(
                    "provider-1", rate, travel, minHours, maxHours, 1, 3, jobs("KITCHEN", "LAUNDRY_FOLD", "TOILET_CARE"), cities, tier,
                    supervisorRule, takesRequests, acceptance);
        }
    }

    /** KITCHEN and LAUNDRY_FOLD, Saturday 10:00, 3 hours, 2 helpers, in Cyberjaya. */
    static final class DetailsBuilder {
        private Set<JobCode> chosenJobs = jobs("KITCHEN", "LAUNDRY_FOLD");
        private Instant start = saturday("10:00");
        private int hours = 3;
        private int helpers = 2;
        private Address address = anAddressIn("Cyberjaya");

        DetailsBuilder forJobs(String... codes) {
            chosenJobs = jobs(codes);
            return this;
        }

        DetailsBuilder startingAt(Instant at) {
            start = at;
            return this;
        }

        DetailsBuilder forHours(int booked) {
            hours = booked;
            return this;
        }

        DetailsBuilder withHelpers(int booked) {
            helpers = booked;
            return this;
        }

        DetailsBuilder in(String city) {
            address = anAddressIn(city);
            return this;
        }

        ReservationDetails build() {
            return new ReservationDetails(chosenJobs, start, hours, helpers, address);
        }
    }

    /** A request made Thursday 09:00 by card, for the booking and offering above, on the launch policy. */
    static final class ReservationBuilder {
        private OfferingBuilder offering = anOffering();
        private DetailsBuilder details = aBooking();
        private Instant requestedAt = thursday("09:00");
        private PolicySnapshot policy = theLaunchPolicy();

        ReservationBuilder underPolicy(PolicySnapshot chosen) {
            policy = chosen;
            return this;
        }

        ReservationBuilder withOffering(OfferingBuilder chosen) {
            offering = chosen;
            return this;
        }

        ReservationBuilder onTier(CancellationTier tier) {
            offering.onTier(tier);
            return this;
        }

        ReservationBuilder withTravelIncluded(String ratePerHelperPerHour) {
            offering.charging(ratePerHelperPerHour).withTravelIncluded();
            return this;
        }

        ReservationBuilder forBooking(DetailsBuilder chosen) {
            details = chosen;
            return this;
        }

        ReservationBuilder requestedAt(Instant at) {
            requestedAt = at;
            return this;
        }

        /** Requested with its payment secured for the total, so the provider can answer it. */
        Reservation requested() {
            Reservation reservation = requestedUnpaid();
            reservation.confirmPayment(reservation.price().total());
            return reservation;
        }

        /** Requested, with its payment not secured yet. */
        Reservation requestedUnpaid() {
            return Reservation.request(
                    "customer-1", details.build(), offering.build(), policy, Optional.empty(), PaymentMethod.CARD, VERIFIED_UNTIL,
                    requestedAt);
        }

        /** Requested, then accepted an hour later. */
        Reservation reserved() {
            Reservation reservation = requested();
            reservation.accept(VERIFIED_UNTIL, requestedAt.plus(Duration.ofHours(1)));
            return reservation;
        }

        /** Reserved, with Aina and Ben named as the two helpers. */
        Reservation reservedWithTeamNamed() {
            Reservation reservation = reserved();
            reservation.nameTeam(TeamAssignment.helpers("aina", "ben"), ROSTER, requestedAt.plus(Duration.ofHours(2)));
            return reservation;
        }

        /** Checked in at Saturday 10:00, with completion submitted at {@code at} with a photo. */
        Reservation completedAt(Instant at) {
            Reservation reservation = checkedInAt(saturday("10:00"));
            reservation.submitCompletion(List.of(A_PHOTO), at);
            return reservation;
        }

        /** Reserved with the team named, the location check passed 120 metres away at {@code at}, and Aina's face check passed then. */
        Reservation checkedInAt(Instant at) {
            Reservation reservation = reservedWithTeamNamed();
            reservation.checkIn(120, at);
            reservation.recordFaceCheck("aina", FaceCheckResult.PASSED, at);
            return reservation;
        }
    }
}
