package verification.usecase.port.in;

import verification.usecase.dto.ReportBusinessSeriousFlagRequest;
import verification.usecase.dto.ReportBusinessSeriousFlagResponse;

/** The KYC provider reports a confirmed serious flag on a business. */
public interface KycBusinessSeriousFlagReporter {

    ReportBusinessSeriousFlagResponse report(ReportBusinessSeriousFlagRequest request);
}
