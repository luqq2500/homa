package verification.usecase;

import java.util.List;
import verification.domain.Business;
import verification.domain.TeamMemberStatus;
import verification.usecase.dto.ViewVerificationRequest;
import verification.usecase.dto.ViewVerificationResponse;
import verification.usecase.dto.ViewVerificationResponse.Person;
import verification.usecase.port.in.ProviderVerificationViewer;
import verification.usecase.port.out.BusinessRepository;
import verification.usecase.port.out.TeamMemberRepository;

public final class ProviderViewVerification implements ProviderVerificationViewer {

    private final BusinessRepository businesses;
    private final TeamMemberRepository members;

    public ProviderViewVerification(BusinessRepository businesses, TeamMemberRepository members) {
        this.businesses = businesses;
        this.members = members;
    }

    /** The business and its people, without those removed or whose check added nothing. */
    @Override
    public ViewVerificationResponse view(ViewVerificationRequest request) {
        Business business = Verifications.business(businesses, request.businessId());
        List<Person> people = members.ofBusiness(business.id()).stream()
                .filter(member -> member.status() != TeamMemberStatus.REMOVED)
                .filter(member -> member.status() != TeamMemberStatus.CHECK_FAILED)
                .map(member -> new Person(member.id(), member.status().name(), member.role().name(),
                        member.registeredName(), member.verifiedUntil(), member.lastVerifiedAt()))
                .toList();
        return new ViewVerificationResponse(business.status().name(), business.registeredName(),
                business.verifiedUntil(), business.lastVerifiedAt(), people);
    }
}
