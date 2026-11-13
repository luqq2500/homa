package reservation.usecase;

import ie.events.FlaggedReservationDecidedIE;
import ie.events.ReservationRestoredIE;
import java.time.Clock;
import java.time.Instant;
import reservation.usecase.port.in.FlaggedReservationDecidedIEHandler;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class HandleFlaggedReservationDecidedIE implements FlaggedReservationDecidedIEHandler {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;
    private final ReservationAnnouncer announcer;

    public HandleFlaggedReservationDecidedIE(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
        this.announcer = new ReservationAnnouncer(reservations, publisher);
    }

    @Override
    public void handle(FlaggedReservationDecidedIE event) {
        reservations.find(event.reservationId()).ifPresent(reservation -> {
            Instant now = clock.instant();
            if (event.restored()) {
                reservation.restoreAfterFlagCleared(now);
                reservations.save(reservation);
                publisher.publish(new ReservationRestoredIE(reservation.id(), reservation.customerId(),
                        reservation.providerId(), now));
            } else {
                reservation.cancelForSeriousFlag();
                reservations.save(reservation);
                announcer.ended(reservation, now);
            }
        });
    }
}
