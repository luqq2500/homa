package offering.usecase;

import ie.events.TeamMemberJoinedIE;
import java.time.Clock;
import offering.domain.Listing;
import offering.domain.TeamRole;
import offering.usecase.port.in.TeamMemberJoinedIEHandler;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class HandleTeamMemberJoinedIE implements TeamMemberJoinedIEHandler {

    private final OfferingRepository offerings;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public HandleTeamMemberJoinedIE(OfferingRepository offerings, OfferingIEPublisher publisher, Clock clock) {
        this.offerings = offerings;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public void handle(TeamMemberJoinedIE event) {
        offerings.find(event.businessId()).ifPresent(offering -> {
            Listing before = offering.listing();
            offering.teamMemberJoined(event.memberId(), TeamRole.valueOf(event.role()));
            Offerings.saveAndAnnounce(offering, before, offerings, publisher, clock.instant());
        });
    }
}
