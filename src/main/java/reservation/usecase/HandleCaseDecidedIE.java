package reservation.usecase;

import ie.events.CaseDecidedIE;
import java.time.Clock;
import reservation.usecase.port.in.CaseDecidedIEHandler;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class HandleCaseDecidedIE implements CaseDecidedIEHandler {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;
    private final ReservationAnnouncer announcer;

    public HandleCaseDecidedIE(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
        this.announcer = new ReservationAnnouncer(reservations, publisher);
    }

    @Override
    public void handle(CaseDecidedIE event) {
        reservations.find(event.reservationId()).ifPresent(reservation -> {
            reservation.decide(event.providerReceives());
            reservations.save(reservation);
            announcer.ended(reservation, clock.instant());
        });
    }
}
