package verification.usecase;

import ie.events.BusinessSeriouslyFlaggedIE;
import java.time.Clock;
import verification.domain.Business;
import verification.usecase.dto.ReportBusinessSeriousFlagRequest;
import verification.usecase.dto.ReportBusinessSeriousFlagResponse;
import verification.usecase.port.in.KycBusinessSeriousFlagReporter;
import verification.usecase.port.out.BusinessRepository;
import verification.usecase.port.out.VerificationIEPublisher;

public final class KycReportBusinessSeriousFlag implements KycBusinessSeriousFlagReporter {

    private final BusinessRepository businesses;
    private final VerificationIEPublisher publisher;
    private final Clock clock;

    public KycReportBusinessSeriousFlag(BusinessRepository businesses, VerificationIEPublisher publisher,
                                        Clock clock) {
        this.businesses = businesses;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public ReportBusinessSeriousFlagResponse report(ReportBusinessSeriousFlagRequest request) {
        Business business = Verifications.business(businesses, request.businessId());
        if (business.flagSeriously()) {
            businesses.save(business);
            publisher.publish(new BusinessSeriouslyFlaggedIE(business.id(), clock.instant()));
        }
        return new ReportBusinessSeriousFlagResponse(business.status().name());
    }
}
