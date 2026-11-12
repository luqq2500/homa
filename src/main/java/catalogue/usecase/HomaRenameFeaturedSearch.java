package catalogue.usecase;

import catalogue.domain.Showcase;
import catalogue.usecase.dto.RenameFeaturedSearchRequest;
import catalogue.usecase.dto.RenameFeaturedSearchResponse;
import catalogue.usecase.port.in.HomaFeaturedSearchRenamer;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.port.out.ShowcaseRepository;
import java.time.Clock;

/**
 * Gives a featured search a new title. Its search and place stay. The same title changes nothing, so
 * nothing is saved or recorded.
 */
public final class HomaRenameFeaturedSearch implements HomaFeaturedSearchRenamer {

    private final ShowcaseRepository showcases;
    private final AdminActions admin;
    private final Clock clock;

    public HomaRenameFeaturedSearch(ShowcaseRepository showcases, AdminAuditLog auditLog, Clock clock) {
        this.showcases = showcases;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public RenameFeaturedSearchResponse rename(RenameFeaturedSearchRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        Showcase showcase = showcases.find().orElseGet(Showcase::create);

        boolean changed = showcase.rename(request.featuredSearchId(), request.title());

        if (changed) {
            showcases.save(showcase);
            admin.record(founder, "RENAME_FEATURED_SEARCH", request.featuredSearchId(), clock.instant());
        }
        String title = showcase.featuredSearch(request.featuredSearchId()).orElseThrow().title();
        return new RenameFeaturedSearchResponse(request.featuredSearchId(), title);
    }
}
