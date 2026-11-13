package verification.usecase;

import java.time.Clock;
import verification.domain.Business;
import verification.usecase.dto.ReportBusinessReverificationNeededRequest;
import verification.usecase.dto.ReportBusinessReverificationNeededResponse;
import verification.usecase.port.in.KycBusinessReverificationNeededReporter;
import verification.usecase.port.out.BusinessRepository;
import verification.usecase.port.out.VerificationIEPublisher;

public final class KycReportBusinessReverificationNeeded implements KycBusinessReverificationNeededReporter {

    private final BusinessRepository businesses;
    private final VerificationIEPublisher publisher;
    private final Clock clock;

    public KycReportBusinessReverificationNeeded(BusinessRepository businesses, VerificationIEPublisher publisher,
                                                 Clock clock) {
        this.businesses = businesses;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public ReportBusinessReverificationNeededResponse report(ReportBusinessReverificationNeededRequest request) {
        Business business = Verifications.business(businesses, request.businessId());
        if (business.reverificationNeededBy(request.neededBy())) {
            businesses.save(business);
            publisher.publish(Verifications.verified(business, clock.instant()));
        }
        return new ReportBusinessReverificationNeededResponse(business.verifiedUntil());
    }
}
