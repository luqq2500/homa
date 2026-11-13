package verification.usecase;

import ie.events.BusinessKybRejectedIE;
import java.time.Clock;
import java.time.Instant;
import verification.domain.Business;
import verification.usecase.dto.ReportBusinessResultRequest;
import verification.usecase.dto.ReportBusinessResultResponse;
import verification.usecase.port.in.KycBusinessResultReporter;
import verification.usecase.port.out.BusinessRepository;
import verification.usecase.port.out.VerificationIEPublisher;

public final class KycReportBusinessResult implements KycBusinessResultReporter {

    private final BusinessRepository businesses;
    private final VerificationIEPublisher publisher;
    private final Clock clock;

    public KycReportBusinessResult(BusinessRepository businesses, VerificationIEPublisher publisher, Clock clock) {
        this.businesses = businesses;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public ReportBusinessResultResponse report(ReportBusinessResultRequest request) {
        Instant now = clock.instant();
        Business business = Verifications.business(businesses, request.businessId());
        if (request.passed()) {
            business.passed(request.ssmNumber(), request.registeredName(), request.verifiedUntil(), now);
            businesses.save(business);
            publisher.publish(Verifications.verified(business, now));
        } else if (business.failed()) {
            businesses.save(business);
            publisher.publish(new BusinessKybRejectedIE(business.id(), now));
        }
        return new ReportBusinessResultResponse(business.status().name());
    }
}
