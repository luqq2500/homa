package verification.usecase;

import verification.domain.Business;
import verification.usecase.dto.StartBusinessCheckRequest;
import verification.usecase.dto.StartBusinessCheckResponse;
import verification.usecase.port.in.ProviderBusinessCheckStarter;
import verification.usecase.port.out.BusinessRepository;
import verification.usecase.port.out.KycProvider;

public final class ProviderStartBusinessCheck implements ProviderBusinessCheckStarter {

    private final BusinessRepository businesses;
    private final KycProvider kyc;

    public ProviderStartBusinessCheck(BusinessRepository businesses, KycProvider kyc) {
        this.businesses = businesses;
        this.kyc = kyc;
    }

    @Override
    public StartBusinessCheckResponse start(StartBusinessCheckRequest request) {
        Business business = Verifications.business(businesses, request.businessId());
        return new StartBusinessCheckResponse(kyc.startBusinessCheck(business.id()));
    }
}
