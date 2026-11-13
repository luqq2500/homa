package verification.usecase;

import java.time.Clock;
import verification.domain.EarlyRecheck;
import verification.usecase.dto.RequestEarlyRecheckRequest;
import verification.usecase.dto.RequestEarlyRecheckResponse;
import verification.usecase.exception.BusinessNotFoundException;
import verification.usecase.port.in.AdminEarlyRecheckRequester;
import verification.usecase.port.out.BusinessRepository;
import verification.usecase.port.out.EarlyRecheckRepository;
import verification.usecase.port.out.KycProvider;
import verification.usecase.port.out.TeamMemberRepository;

public final class AdminRequestEarlyRecheck implements AdminEarlyRecheckRequester {

    private final BusinessRepository businesses;
    private final TeamMemberRepository members;
    private final EarlyRecheckRepository rechecks;
    private final KycProvider kyc;
    private final Clock clock;

    public AdminRequestEarlyRecheck(BusinessRepository businesses, TeamMemberRepository members,
                                    EarlyRecheckRepository rechecks, KycProvider kyc, Clock clock) {
        this.businesses = businesses;
        this.members = members;
        this.rechecks = rechecks;
        this.kyc = kyc;
        this.clock = clock;
    }

    @Override
    public RequestEarlyRecheckResponse request(RequestEarlyRecheckRequest request) {
        String subjectId = request.subjectId();
        if (businesses.find(subjectId).isEmpty() && members.find(subjectId).isEmpty()) {
            throw new BusinessNotFoundException(subjectId);
        }
        EarlyRecheck recheck = EarlyRecheck.request(subjectId, request.reason(), request.adminId(), clock.instant());
        rechecks.save(recheck);
        kyc.requestEarlyRecheck(subjectId, recheck.reason());
        return new RequestEarlyRecheckResponse(recheck.id());
    }
}
