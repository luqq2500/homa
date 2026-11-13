package reservation.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import reservation.domain.Reservation;
import reservation.usecase.dto.AcceptRequestRequest;
import reservation.usecase.dto.AcceptRequestResponse;
import reservation.usecase.port.in.ProviderRequestAccepter;
import reservation.usecase.port.out.ProviderOpenTimeRepository;
import reservation.usecase.port.out.ProviderStandingRepository;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class ProviderAcceptRequest implements ProviderRequestAccepter {

    private final ReservationRepository reservations;
    private final ProviderOpenTimeRepository openTimes;
    private final ProviderStandingRepository standings;
    private final ReservationIEPublisher publisher;
    private final Clock clock;
    private final ProviderScheduleReader schedules;
    private final ReservationAnnouncer announcer;

    public ProviderAcceptRequest(ReservationRepository reservations, ProviderOpenTimeRepository openTimes, ProviderStandingRepository standings, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.openTimes = openTimes;
        this.standings = standings;
        this.publisher = publisher;
        this.clock = clock;
        this.schedules = new ProviderScheduleReader(openTimes, standings, reservations);
        this.announcer = new ReservationAnnouncer(reservations, publisher);
    }

    @Override
    public AcceptRequestResponse accept(AcceptRequestRequest request) {
        Instant now = clock.instant();
        Reservation reservation =
                OwnedReservations.ofProvider(reservations, request.providerId(), request.reservationId());
        reservation.accept(schedules.standingOf(reservation.providerId()),
                schedules.scheduleOf(reservation.providerId(), reservation.id()), now);
        reservations.save(reservation);
        List<String> openBefore = openRequestsOf(reservation.providerId());
        announcer.accepted(reservation, schedules, now);
        openBefore.removeAll(openRequestsOf(reservation.providerId()));
        return new AcceptRequestResponse(reservation.id(), openBefore);
    }

    private List<String> openRequestsOf(String providerId) {
        return new ArrayList<>(reservations.ofProvider(providerId).stream()
                .filter(Reservation::isRequested).map(Reservation::id).toList());
    }
}
