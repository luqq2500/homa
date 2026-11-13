package offering.usecase;

import ie.events.TeamMemberLeftIE;
import java.time.Clock;
import offering.domain.Listing;
import offering.usecase.port.in.TeamMemberLeftIEHandler;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class HandleTeamMemberLeftIE implements TeamMemberLeftIEHandler {

    private final OfferingRepository offerings;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public HandleTeamMemberLeftIE(OfferingRepository offerings, OfferingIEPublisher publisher, Clock clock) {
        this.offerings = offerings;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public void handle(TeamMemberLeftIE event) {
        offerings.find(event.businessId()).ifPresent(offering -> {
            Listing before = offering.listing();
            offering.teamMemberLeft(event.memberId());
            Offerings.saveAndAnnounce(offering, before, offerings, publisher, clock.instant());
        });
    }
}
