package reservation.support;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kernel.City;
import kernel.GeoPoint;
import kernel.Money;
import kernel.Percentage;
import kernel.TimeRange;
import reservation.domain.AcceptancePolicy;
import reservation.domain.Address;
import reservation.domain.BookingDetails;
import reservation.domain.CancellationTier;
import reservation.domain.Customer;
import reservation.domain.FaceCheckResult;
import reservation.domain.HomaValues;
import reservation.domain.ProviderSchedule;
import reservation.domain.ProviderStanding;
import reservation.domain.ProviderTerms;
import reservation.domain.Reservation;
import reservation.domain.SupervisorRule;
import reservation.domain.TeamRole;
import reservation.domain.TeamRoster;

/**
 * Ready-made reservations for tests. The standard booking is 2 helpers for 3 hours at RM30 an hour
 * with a separate RM10 travel fee: hours fee RM180, provider price RM190, service fee RM28.50,
 * total RM218.50; customer no-show fee RM64.
 */
public final class ReservationFixtures {

    public static final Instant NOW = Instant.parse("2026-03-07T02:00:00Z");
    /** The booked start: three days from now, 10:00 in Malaysia. */
    public static final Instant START = Instant.parse("2026-03-10T02:00:00Z");
    public static final Instant END = START.plus(Duration.ofHours(3));
    public static final String PROVIDER = "provider-1";
    public static final String CUSTOMER = "customer-1";
    public static final String KITCHEN = "job-kitchen";
    public static final String LAUNDRY = "job-laundry";
    public static final Address HOME = new Address("12 Jalan Teknokrat 5", new GeoPoint(2.9213, 101.6559), City.CYBERJAYA);
    public static final HomaValues VALUES = new HomaValues(1, Percentage.of(15), 1, 6, Duration.ofHours(2),
            Duration.ofDays(30), Duration.ofHours(6), Duration.ofMinutes(15), Duration.ofMinutes(15), 300,
            Duration.ofMinutes(15), Percentage.of(30), Duration.ofHours(1), Duration.ofHours(24), Duration.ofHours(1),
            Duration.ofMinutes(30), Duration.ofHours(24), Duration.ofHours(24), Duration.ofHours(24),
            Percentage.of(10), Percentage.of(20), Percentage.of(20), Percentage.of(30));
    public static final BookingDetails DETAILS = new BookingDetails(Set.of(KITCHEN), START, 3, 2);
    public static final ProviderStanding VERIFIED = new ProviderStanding(PROVIDER, NOW.plus(Duration.ofDays(365)), false);
    public static final TeamRoster ROSTER = new TeamRoster(PROVIDER, Map.of("helper-1", TeamRole.HELPER,
            "helper-2", TeamRole.HELPER, "helper-3", TeamRole.HELPER, "supervisor-1", TeamRole.SUPERVISOR));

    private ReservationFixtures() {
    }

    public static Customer customer() {
        return new Customer(CUSTOMER, "Aisyah binti Rahman", HOME, "Unit 12-3, register at the guardhouse");
    }

    public static ProviderTerms terms(CancellationTier tier, AcceptancePolicy policy) {
        return new ProviderTerms(PROVIDER, true, Set.of(KITCHEN, LAUNDRY), 1, 3, new SupervisorRule(3, 5),
                Money.ofRinggit(30), true, Money.ofRinggit(10), 2, 5, Set.of(City.CYBERJAYA, City.PUTRAJAYA), tier,
                policy);
    }

    public static ProviderTerms terms() {
        return terms(CancellationTier.STANDARD, AcceptancePolicy.REQUEST_AND_ACCEPT);
    }

    /** Open for the next forty days, with a 30-minute buffer and no accepted job. */
    public static ProviderSchedule freeSchedule() {
        return schedule(List.of());
    }

    public static ProviderSchedule schedule(List<TimeRange> acceptedJobs) {
        return new ProviderSchedule(List.of(TimeRange.of(NOW, NOW.plus(Duration.ofDays(40)))), Duration.ofMinutes(30),
                acceptedJobs);
    }

    public static Reservation requested(ProviderTerms terms) {
        return Reservation.request(customer(), terms, VERIFIED, freeSchedule(), DETAILS, VALUES, NOW);
    }

    public static Reservation requested() {
        return requested(terms());
    }

    /** Requested with its payment secured: the provider can now answer. */
    public static Reservation secured(ProviderTerms terms) {
        Reservation reservation = requested(terms);
        reservation.securePayment();
        return reservation;
    }

    public static Reservation reserved(ProviderTerms terms) {
        Reservation reservation = secured(terms);
        reservation.accept(VERIFIED, freeSchedule(), NOW);
        return reservation;
    }

    public static Reservation reserved() {
        return reserved(terms());
    }

    public static Reservation teamNamed() {
        Reservation reservation = reserved();
        reservation.nameJobTeam(Set.of("helper-1", "helper-2"), ROSTER, NOW);
        return reservation;
    }

    /** Checked in at the booked start. */
    public static Reservation checkedIn() {
        Reservation reservation = teamNamed();
        reservation.checkIn("helper-1", FaceCheckResult.PASSED, true, START);
        return reservation;
    }

    /** Completion submitted at the booked end. */
    public static Reservation completionSubmitted() {
        Reservation reservation = checkedIn();
        reservation.submitCompletion(List.of("evidence/kitchen.jpg"), END);
        return reservation;
    }
}
