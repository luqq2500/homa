package catalogue.usecase;

import catalogue.domain.FeaturedSearch;
import catalogue.domain.Showcase;
import catalogue.usecase.dto.MoveFeaturedSearchRequest;
import catalogue.usecase.dto.MoveFeaturedSearchResponse;
import catalogue.usecase.port.in.HomaFeaturedSearchMover;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.port.out.ShowcaseRepository;
import java.time.Clock;

/**
 * Moves a featured search to another place in the showcase; the ones in between move along. Its own place
 * changes nothing, so nothing is saved or recorded.
 */
public final class HomaMoveFeaturedSearch implements HomaFeaturedSearchMover {

    private final ShowcaseRepository showcases;
    private final AdminActions admin;
    private final Clock clock;

    public HomaMoveFeaturedSearch(ShowcaseRepository showcases, AdminAuditLog auditLog, Clock clock) {
        this.showcases = showcases;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public MoveFeaturedSearchResponse move(MoveFeaturedSearchRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        Showcase showcase = showcases.find().orElseGet(Showcase::create);

        boolean moved = showcase.move(request.featuredSearchId(), request.position());

        if (moved) {
            showcases.save(showcase);
            admin.record(founder, "MOVE_FEATURED_SEARCH", request.featuredSearchId(), clock.instant());
        }
        return new MoveFeaturedSearchResponse(showcase.featuredSearches().stream().map(FeaturedSearch::id).toList());
    }
}
