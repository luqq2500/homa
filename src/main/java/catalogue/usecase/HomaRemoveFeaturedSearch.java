package catalogue.usecase;

import catalogue.domain.FeaturedSearch;
import catalogue.domain.Showcase;
import catalogue.usecase.dto.RemoveFeaturedSearchRequest;
import catalogue.usecase.dto.RemoveFeaturedSearchResponse;
import catalogue.usecase.port.in.HomaFeaturedSearchRemover;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.port.out.ShowcaseRepository;
import java.time.Clock;

/** Takes a featured search off the showcase; the ones after it move up one. */
public final class HomaRemoveFeaturedSearch implements HomaFeaturedSearchRemover {

    private final ShowcaseRepository showcases;
    private final AdminActions admin;
    private final Clock clock;

    public HomaRemoveFeaturedSearch(ShowcaseRepository showcases, AdminAuditLog auditLog, Clock clock) {
        this.showcases = showcases;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public RemoveFeaturedSearchResponse remove(RemoveFeaturedSearchRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        Showcase showcase = showcases.find().orElseGet(Showcase::create);

        showcase.remove(request.featuredSearchId());

        showcases.save(showcase);
        admin.record(founder, "REMOVE_FEATURED_SEARCH", request.featuredSearchId(), clock.instant());
        return new RemoveFeaturedSearchResponse(showcase.featuredSearches().stream().map(FeaturedSearch::id).toList());
    }
}
