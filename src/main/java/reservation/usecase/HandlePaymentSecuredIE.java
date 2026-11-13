package reservation.usecase;

import ie.events.PaymentSecuredIE;
import java.time.Clock;
import java.time.Instant;
import reservation.domain.Reservation;
import reservation.usecase.port.in.PaymentSecuredIEHandler;
import reservation.usecase.port.out.ProviderOpenTimeRepository;
import reservation.usecase.port.out.ProviderStandingRepository;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class HandlePaymentSecuredIE implements PaymentSecuredIEHandler {

    private final ReservationRepository reservations;
    private final ProviderOpenTimeRepository openTimes;
    private final ProviderStandingRepository standings;
    private final ReservationIEPublisher publisher;
    private final Clock clock;
    private final ProviderScheduleReader schedules;
    private final ReservationAnnouncer announcer;

    public HandlePaymentSecuredIE(ReservationRepository reservations, ProviderOpenTimeRepository openTimes, ProviderStandingRepository standings, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.openTimes = openTimes;
        this.standings = standings;
        this.publisher = publisher;
        this.clock = clock;
        this.schedules = new ProviderScheduleReader(openTimes, standings, reservations);
        this.announcer = new ReservationAnnouncer(reservations, publisher);
    }

    @Override
    public void handle(PaymentSecuredIE event) {
        reservations.find(event.reservationId()).filter(Reservation::isRequested).ifPresent(this::secure);
    }

    /** With Instant book the request is accepted the moment its payment is secured, if the time is still free. */
    private void secure(Reservation reservation) {
        Instant now = clock.instant();
        reservation.securePayment();
        if (!reservation.state().terms().usesInstantBook()) {
            reservations.save(reservation);
            return;
        }
        boolean accepted = reservation.acceptInstantly(schedules.standingOf(reservation.providerId()),
                schedules.scheduleOf(reservation.providerId(), reservation.id()), now);
        reservations.save(reservation);
        if (accepted) {
            announcer.accepted(reservation, schedules, now);
        } else {
            announcer.ended(reservation, now);
        }
    }
}
