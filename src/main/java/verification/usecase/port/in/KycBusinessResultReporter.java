package verification.usecase.port.in;

import verification.usecase.dto.ReportBusinessResultRequest;
import verification.usecase.dto.ReportBusinessResultResponse;

/** The KYC provider reports the final result of a business check. */
public interface KycBusinessResultReporter {

    ReportBusinessResultResponse report(ReportBusinessResultRequest request);
}
