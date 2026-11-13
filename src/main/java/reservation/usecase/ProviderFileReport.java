package reservation.usecase;

import ie.events.ReportFiledIE;
import java.time.Clock;
import java.time.Instant;
import reservation.domain.Report;
import reservation.domain.ReportingParty;
import reservation.domain.Reservation;
import reservation.usecase.dto.FileProviderReportRequest;
import reservation.usecase.dto.FileProviderReportResponse;
import reservation.usecase.port.in.ProviderReportFiler;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class ProviderFileReport implements ProviderReportFiler {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;

    public ProviderFileReport(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public FileProviderReportResponse file(FileProviderReportRequest request) {
        Instant now = clock.instant();
        Reservation reservation =
                OwnedReservations.ofProvider(reservations, request.providerId(), request.reservationId());
        Report report = new Report(ReportingParty.PROVIDER, ReportTypes.called(request.type()), request.reason(),
                request.evidence(), now);
        reservation.fileReport(report);
        reservations.save(reservation);
        publisher.publish(new ReportFiledIE(reservation.id(), reservation.customerId(), reservation.providerId(),
                report.party().name(), report.type().name(), report.reason(), report.evidence(),
                reservation.atStake(), now));
        return new FileProviderReportResponse(reservation.id(), reservation.status().name());
    }
}
