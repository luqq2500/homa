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

    /** Homa's launch values, as policy version 1. */
    static PolicySnapshot theLaunchPolicy() {
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
                Duration.ofHours(6));
    }

    static final EvidenceRef A_PHOTO = EvidenceRef.of("evidence/kitchen-after.jpg");

    /** The provider's verified roster: Aina, Ben and Chan. */
    static final TeamRoster ROSTER = TeamRoster.of("aina", "ben", "chan");

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
                    "provider-1", rate, travel, 2, 4, 1, 3, jobs("KITCHEN", "LAUNDRY_FOLD", "TOILET_CARE"), cities, tier,
                    takesRequests);
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

        Reservation requested() {
            return Reservation.request(
                    "customer-1", details.build(), offering.build(), theLaunchPolicy(), Optional.empty(), PaymentMethod.CARD, requestedAt);
        }

        /** Requested, then accepted an hour later. */
        Reservation reserved() {
            Reservation reservation = requested();
            reservation.accept(requestedAt.plus(Duration.ofHours(1)));
            return reservation;
        }

        /** Reserved, with Aina as lead and Aina and Ben named as the two helpers. */
        Reservation reservedWithTeamNamed() {
            Reservation reservation = reserved();
            reservation.nameTeam(TeamAssignment.of("aina", "aina", "ben"), ROSTER, requestedAt.plus(Duration.ofHours(2)));
            return reservation;
        }

        /** Checked in at Saturday 10:00, with completion submitted at {@code at} with a photo. */
        Reservation completedAt(Instant at) {
            Reservation reservation = checkedInAt(saturday("10:00"));
            reservation.submitCompletion(List.of(A_PHOTO), at);
            return reservation;
        }

        /** Reserved with the team named, and the lead checked in 120 metres away at {@code at}. */
        Reservation checkedInAt(Instant at) {
            Reservation reservation = reservedWithTeamNamed();
            reservation.checkIn(120, at);
            return reservation;
        }
    }
}
