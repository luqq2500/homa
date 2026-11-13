package verification.usecase.port.in;

import verification.usecase.dto.ReportBusinessLapseRequest;
import verification.usecase.dto.ReportBusinessLapseResponse;

/** The KYC provider reports that a business missed its re-verification. */
public interface KycBusinessLapseReporter {

    ReportBusinessLapseResponse report(ReportBusinessLapseRequest request);
}
