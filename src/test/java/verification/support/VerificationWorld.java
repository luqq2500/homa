package verification.support;

import static verification.support.VerificationFixtures.NEXT_YEAR;
import static verification.support.VerificationFixtures.NOW;
import static verification.support.VerificationFixtures.PHOTO;

import ie.events.BusinessRegisteredIE;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import verification.usecase.HandleBusinessRegisteredIE;
import verification.usecase.KycReportBusinessResult;
import verification.usecase.KycReportTeamMemberResult;
import verification.usecase.ProviderConfirmTeamMember;
import verification.usecase.ProviderStartTeamMember;
import verification.usecase.dto.ConfirmTeamMemberRequest;
import verification.usecase.dto.ReportBusinessResultRequest;
import verification.usecase.dto.ReportTeamMemberResultRequest;
import verification.usecase.dto.StartTeamMemberRequest;
import verification.usecase.port.out.KycProvider;

/** The verification context's ports, faked in memory, with shortcuts to reach the state a test needs. */
public final class VerificationWorld {

    public final InMemoryBusinessRepository businesses = new InMemoryBusinessRepository();
    public final InMemoryTeamMemberRepository members = new InMemoryTeamMemberRepository();
    public final InMemoryEarlyRecheckRepository rechecks = new InMemoryEarlyRecheckRepository();
    public final RecordingVerificationIEPublisher publisher = new RecordingVerificationIEPublisher();
    public final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    /** What Homa asked of the KYC provider, in order. */
    public final List<String> kycCalls = new ArrayList<>();
    public final KycProvider kyc = new KycProvider() {
        @Override
        public String startBusinessCheck(String businessId) {
            kycCalls.add("business-check " + businessId);
            return "session-for-" + businessId;
        }

        @Override
        public String startTeamMemberCheck(String memberId, String profilePhoto) {
            kycCalls.add("member-check " + memberId + " " + profilePhoto);
            return "session-for-" + memberId;
        }

        @Override
        public void requestEarlyRecheck(String subjectId, String reason) {
            kycCalls.add("early-recheck " + subjectId + " " + reason);
        }
    };

    public String registeredBusiness(String businessId) {
        new HandleBusinessRegisteredIE(businesses).handle(new BusinessRegisteredIE(businessId, "MALAY", NOW));
        return businessId;
    }

    public String verifiedBusiness(String businessId) {
        registeredBusiness(businessId);
        new KycReportBusinessResult(businesses, publisher, clock).report(
                new ReportBusinessResultRequest(businessId, true, "202401012345", "Bersih Sdn Bhd", NEXT_YEAR));
        return businessId;
    }

    public String startedPerson(String businessId, String role) {
        return new ProviderStartTeamMember(businesses, members, kyc)
                .start(new StartTeamMemberRequest(businessId, role, PHOTO)).memberId();
    }

    public String verifiedPerson(String businessId, String role) {
        String memberId = startedPerson(businessId, role);
        new KycReportTeamMemberResult(members, publisher, clock).report(
                new ReportTeamMemberResultRequest(memberId, "PASSED", "Siti binti Ahmad", "MY", NEXT_YEAR));
        return memberId;
    }

    public String teamMember(String businessId, String role) {
        String memberId = verifiedPerson(businessId, role);
        new ProviderConfirmTeamMember(members, publisher, clock)
                .confirm(new ConfirmTeamMemberRequest(businessId, memberId, true, false, "terms-v1"));
        return memberId;
    }
}
