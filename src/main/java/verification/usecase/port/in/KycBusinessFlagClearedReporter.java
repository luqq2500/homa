package verification.usecase.port.in;

import verification.usecase.dto.ReportBusinessFlagClearedRequest;
import verification.usecase.dto.ReportBusinessFlagClearedResponse;

/** The KYC provider reports that it cleared the serious flag on a business. */
public interface KycBusinessFlagClearedReporter {

    ReportBusinessFlagClearedResponse report(ReportBusinessFlagClearedRequest request);
}
