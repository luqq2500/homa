package reservation.usecase;

import ie.events.CustomerNoShowReportedIE;
import java.time.Clock;
import java.time.Instant;
import reservation.domain.Reservation;
import reservation.usecase.dto.ReportCustomerNoShowRequest;
import reservation.usecase.dto.ReportCustomerNoShowResponse;
import reservation.usecase.port.in.ProviderCustomerNoShowReporter;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class ProviderReportCustomerNoShow implements ProviderCustomerNoShowReporter {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;
    private final ReservationAnnouncer announcer;

    public ProviderReportCustomerNoShow(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
        this.announcer = new ReservationAnnouncer(reservations, publisher);
    }

    @Override
    public ReportCustomerNoShowResponse report(ReportCustomerNoShowRequest request) {
        Instant now = clock.instant();
        Reservation reservation =
                OwnedReservations.ofProvider(reservations, request.providerId(), request.reservationId());
        reservation.reportCustomerNoShow(now);
        reservations.save(reservation);
        Instant closesAt = reservation.noShowReportWindowClosesAt().orElseThrow();
        announcer.ended(reservation, now);
        publisher.publish(new CustomerNoShowReportedIE(reservation.id(), reservation.customerId(),
                reservation.providerId(), closesAt, now));
        return new ReportCustomerNoShowResponse(reservation.id(), reservation.customerNoShowFee(), closesAt);
    }
}
