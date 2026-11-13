package reservation.usecase;

import ie.events.ReportFiledIE;
import java.time.Clock;
import java.time.Instant;
import reservation.domain.Report;
import reservation.domain.ReportingParty;
import reservation.domain.Reservation;
import reservation.usecase.dto.FileCustomerReportRequest;
import reservation.usecase.dto.FileCustomerReportResponse;
import reservation.usecase.port.in.CustomerReportFiler;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class CustomerFileReport implements CustomerReportFiler {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;

    public CustomerFileReport(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public FileCustomerReportResponse file(FileCustomerReportRequest request) {
        Instant now = clock.instant();
        Reservation reservation =
                OwnedReservations.ofCustomer(reservations, request.customerId(), request.reservationId());
        Report report = new Report(ReportingParty.CUSTOMER, ReportTypes.called(request.type()), request.reason(),
                request.evidence(), now);
        reservation.fileReport(report);
        reservations.save(reservation);
        publisher.publish(new ReportFiledIE(reservation.id(), reservation.customerId(), reservation.providerId(),
                report.party().name(), report.type().name(), report.reason(), report.evidence(),
                reservation.atStake(), now));
        return new FileCustomerReportResponse(reservation.id(), reservation.status().name());
    }
}
