package offering.usecase;

import ie.events.TeamMemberRoleChangedIE;
import java.time.Clock;
import offering.domain.Listing;
import offering.domain.TeamRole;
import offering.usecase.port.in.TeamMemberRoleChangedIEHandler;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class HandleTeamMemberRoleChangedIE implements TeamMemberRoleChangedIEHandler {

    private final OfferingRepository offerings;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public HandleTeamMemberRoleChangedIE(OfferingRepository offerings, OfferingIEPublisher publisher, Clock clock) {
        this.offerings = offerings;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public void handle(TeamMemberRoleChangedIE event) {
        offerings.find(event.businessId()).ifPresent(offering -> {
            Listing before = offering.listing();
            offering.teamMemberChangedRole(event.memberId(), TeamRole.valueOf(event.role()));
            Offerings.saveAndAnnounce(offering, before, offerings, publisher, clock.instant());
        });
    }
}
