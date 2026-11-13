package verification.support;

import java.time.Duration;
import java.time.Instant;
import verification.domain.Business;
import verification.domain.Role;
import verification.domain.TeamMember;

/** Ready-made businesses and people for tests. */
public final class VerificationFixtures {

    public static final Instant NOW = Instant.parse("2026-03-07T02:00:00Z");
    public static final Instant NEXT_YEAR = NOW.plus(Duration.ofDays(365));
    public static final String BUSINESS_ID = "business-1";
    public static final String PHOTO = "photos/siti.jpg";

    private VerificationFixtures() {
    }

    public static Business verifiedBusiness() {
        Business business = Business.register(BUSINESS_ID);
        business.passed("202401012345", "Bersih Sdn Bhd", NEXT_YEAR, NOW);
        return business;
    }

    public static TeamMember waitingForVerification(Role role) {
        return TeamMember.start(verifiedBusiness(), role, PHOTO);
    }

    public static TeamMember waitingForConfirmation(Role role) {
        TeamMember member = waitingForVerification(role);
        member.verified("Siti binti Ahmad", "MY", NEXT_YEAR, NOW);
        return member;
    }

    public static TeamMember onTeam(Role role) {
        TeamMember member = waitingForConfirmation(role);
        member.confirm(true, false, "terms-v1", NOW);
        return member;
    }
}
