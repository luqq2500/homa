package offering.usecase.port.in;

import offering.usecase.dto.DeclareInsuranceRequest;
import offering.usecase.dto.DeclareInsuranceResponse;

/** A provider declares their liability insurance: the insurer and the date it is valid until. */
public interface ProviderInsuranceDeclarer {

    DeclareInsuranceResponse declare(DeclareInsuranceRequest request);
}
