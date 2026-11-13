package verification.usecase.port.in;

import verification.usecase.dto.ReportBusinessReverificationNeededRequest;
import verification.usecase.dto.ReportBusinessReverificationNeededResponse;

/** The KYC provider reports that a business must be re-verified by a date. */
public interface KycBusinessReverificationNeededReporter {

    ReportBusinessReverificationNeededResponse report(ReportBusinessReverificationNeededRequest request);
}
