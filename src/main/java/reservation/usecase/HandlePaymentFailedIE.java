package reservation.usecase;

import ie.events.PaymentFailedIE;
import java.time.Clock;
import reservation.domain.Reservation;
import reservation.usecase.port.in.PaymentFailedIEHandler;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class HandlePaymentFailedIE implements PaymentFailedIEHandler {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;
    private final ReservationAnnouncer announcer;

    public HandlePaymentFailedIE(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
        this.announcer = new ReservationAnnouncer(reservations, publisher);
    }

    @Override
    public void handle(PaymentFailedIE event) {
        reservations.find(event.reservationId()).filter(Reservation::isRequested).ifPresent(reservation -> {
            reservation.failPayment();
            reservations.save(reservation);
            announcer.ended(reservation, clock.instant());
        });
    }
}
