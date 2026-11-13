package reservation.support;

import static reservation.support.ReservationFixtures.CUSTOMER;
import static reservation.support.ReservationFixtures.END;
import static reservation.support.ReservationFixtures.KITCHEN;
import static reservation.support.ReservationFixtures.NOW;
import static reservation.support.ReservationFixtures.PROVIDER;
import static reservation.support.ReservationFixtures.START;

import ie.events.PaymentSecuredIE;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import kernel.GeoPoint;
import kernel.TimeRange;
import reservation.domain.FaceCheckResult;
import reservation.domain.HomaValues;
import reservation.domain.ProviderOpenTime;
import reservation.domain.ProviderTerms;
import reservation.domain.Reservation;
import reservation.usecase.CustomerSubmitRequest;
import reservation.usecase.HandlePaymentSecuredIE;
import reservation.usecase.ProviderAcceptRequest;
import reservation.usecase.ProviderCheckIn;
import reservation.usecase.ProviderNameJobTeam;
import reservation.usecase.ProviderSubmitCompletion;
import reservation.usecase.dto.AcceptRequestRequest;
import reservation.usecase.dto.CheckInRequest;
import reservation.usecase.dto.NameJobTeamRequest;
import reservation.usecase.dto.SubmitCompletionRequest;
import reservation.usecase.dto.SubmitRequestRequest;
import reservation.usecase.port.out.FaceChecker;
import reservation.usecase.port.out.HomaValuesRepository;
import reservation.usecase.port.out.LocationChecker;
import support.TestClock;

/** The reservation context's ports, faked in memory, with shortcuts to reach the state a test needs. */
public final class ReservationWorld {

    public static final GeoPoint AT_THE_DOOR = new GeoPoint(2.9214, 101.6560);

    public final InMemoryReservationRepository reservations = new InMemoryReservationRepository();
    public final InMemoryEnquiryRepository enquiries = new InMemoryEnquiryRepository();
    public final InMemoryCustomerRepository customers = new InMemoryCustomerRepository();
    public final InMemoryProviderTermsRepository providerTerms = new InMemoryProviderTermsRepository();
    public final InMemoryProviderStandingRepository standings = new InMemoryProviderStandingRepository();
    public final InMemoryProviderOpenTimeRepository openTimes = new InMemoryProviderOpenTimeRepository();
    public final InMemoryTeamRosterRepository rosters = new InMemoryTeamRosterRepository();
    public final RecordingReservationIEPublisher publisher = new RecordingReservationIEPublisher();
    public final TestClock clock = new TestClock(NOW);
    public Optional<HomaValues> announcedValues = Optional.of(ReservationFixtures.VALUES);
    public final HomaValuesRepository homaValues = new HomaValuesRepository() {
        @Override
        public Optional<HomaValues> current() {
            return announcedValues;
        }

        @Override
        public void save(HomaValues values) {
            announcedValues = Optional.of(values);
        }
    };
    /** Whether the geoservice finds the device within the check-in radius. */
    public boolean withinRadius = true;
    public int radiusAsked;
    public final LocationChecker locations = (device, home, radiusMetres) -> {
        radiusAsked = radiusMetres;
        return withinRadius;
    };
    /** The KYC provider's face check answer per person; anyone not listed passes. */
    public final Map<String, FaceCheckResult> faceAnswers = new HashMap<>();
    public final FaceChecker faces = (memberId, selfie) -> faceAnswers.getOrDefault(memberId, FaceCheckResult.PASSED);

    /** A customer, a live verified provider with a team, their open time and Homa's values, all known. */
    public ReservationWorld() {
        customers.save(ReservationFixtures.customer());
        offering(ReservationFixtures.terms());
        standings.save(ReservationFixtures.VERIFIED);
        openTimes.save(new ProviderOpenTime(PROVIDER,
                List.of(TimeRange.of(NOW, NOW.plus(Duration.ofDays(40)))), Duration.ofMinutes(30)));
        rosters.save(ReservationFixtures.ROSTER);
    }

    public void offering(ProviderTerms terms) {
        providerTerms.save(terms);
    }

    public CustomerSubmitRequest submitter() {
        return new CustomerSubmitRequest(reservations, enquiries, customers, providerTerms, standings, openTimes,
                homaValues, publisher, clock);
    }

    public String requested(Instant start) {
        return submitter().submit(new SubmitRequestRequest(CUSTOMER, PROVIDER, null, "CARD", Set.of(KITCHEN), start,
                3, 2)).reservationId();
    }

    public String requested() {
        return requested(START);
    }

    public String secured(Instant start) {
        String id = requested(start);
        new HandlePaymentSecuredIE(reservations, openTimes, standings, publisher, clock)
                .handle(new PaymentSecuredIE(id, clock.instant()));
        return id;
    }

    public String reserved() {
        return reserved(START);
    }

    public String reserved(Instant start) {
        String id = secured(start);
        new ProviderAcceptRequest(reservations, openTimes, standings, publisher, clock)
                .accept(new AcceptRequestRequest(PROVIDER, id));
        return id;
    }

    public String teamNamed() {
        String id = reserved();
        new ProviderNameJobTeam(reservations, rosters, publisher, clock)
                .name(new NameJobTeamRequest(PROVIDER, id, Set.of("helper-1", "helper-2")));
        return id;
    }

    /** Checked in at the booked start; the clock stays there. */
    public String checkedIn() {
        String id = teamNamed();
        clock.set(START);
        new ProviderCheckIn(reservations, locations, faces, publisher, clock)
                .checkIn(new CheckInRequest(PROVIDER, id, "helper-1", "selfie-1", AT_THE_DOOR));
        return id;
    }

    /** Completion submitted at the booked end; the clock stays there. */
    public String completionSubmitted() {
        String id = checkedIn();
        clock.set(END);
        new ProviderSubmitCompletion(reservations, publisher, clock)
                .submit(new SubmitCompletionRequest(PROVIDER, id, List.of("evidence/kitchen.jpg")));
        return id;
    }

    public Reservation reservation(String id) {
        return reservations.find(id).orElseThrow();
    }
}
