package verification.usecase;

import ie.events.BusinessLapsedIE;
import java.time.Clock;
import verification.domain.Business;
import verification.usecase.dto.ReportBusinessLapseRequest;
import verification.usecase.dto.ReportBusinessLapseResponse;
import verification.usecase.port.in.KycBusinessLapseReporter;
import verification.usecase.port.out.BusinessRepository;
import verification.usecase.port.out.VerificationIEPublisher;

public final class KycReportBusinessLapse implements KycBusinessLapseReporter {

    private final BusinessRepository businesses;
    private final VerificationIEPublisher publisher;
    private final Clock clock;

    public KycReportBusinessLapse(BusinessRepository businesses, VerificationIEPublisher publisher, Clock clock) {
        this.businesses = businesses;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public ReportBusinessLapseResponse report(ReportBusinessLapseRequest request) {
        Business business = Verifications.business(businesses, request.businessId());
        if (business.lapse()) {
            businesses.save(business);
            publisher.publish(new BusinessLapsedIE(business.id(), clock.instant()));
        }
        return new ReportBusinessLapseResponse(business.status().name());
    }
}
