package verification.usecase;

import ie.events.BusinessFlagClearedIE;
import java.time.Clock;
import verification.domain.Business;
import verification.usecase.dto.ReportBusinessFlagClearedRequest;
import verification.usecase.dto.ReportBusinessFlagClearedResponse;
import verification.usecase.port.in.KycBusinessFlagClearedReporter;
import verification.usecase.port.out.BusinessRepository;
import verification.usecase.port.out.VerificationIEPublisher;

public final class KycReportBusinessFlagCleared implements KycBusinessFlagClearedReporter {

    private final BusinessRepository businesses;
    private final VerificationIEPublisher publisher;
    private final Clock clock;

    public KycReportBusinessFlagCleared(BusinessRepository businesses, VerificationIEPublisher publisher,
                                        Clock clock) {
        this.businesses = businesses;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public ReportBusinessFlagClearedResponse report(ReportBusinessFlagClearedRequest request) {
        Business business = Verifications.business(businesses, request.businessId());
        if (business.clearFlag()) {
            businesses.save(business);
            publisher.publish(new BusinessFlagClearedIE(business.id(), clock.instant()));
        }
        return new ReportBusinessFlagClearedResponse(business.status().name());
    }
}
