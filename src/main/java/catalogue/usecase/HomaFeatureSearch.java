package catalogue.usecase;

import catalogue.domain.FeaturedSearch;
import catalogue.domain.JobCode;
import catalogue.domain.JobList;
import catalogue.domain.PresetSearch;
import catalogue.domain.Showcase;
import catalogue.usecase.dto.FeatureSearchRequest;
import catalogue.usecase.dto.FeatureSearchResponse;
import catalogue.usecase.port.in.HomaSearchFeaturer;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.port.out.JobListRepository;
import catalogue.usecase.port.out.ShowcaseRepository;
import java.time.Clock;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Features a search on the homepage, for jobs providers can offer now, at a place in the showcase; the
 * featured searches from that place on move down one.
 */
public final class HomaFeatureSearch implements HomaSearchFeaturer {

    private final ShowcaseRepository showcases;
    private final JobListRepository jobLists;
    private final AdminActions admin;
    private final Clock clock;

    public HomaFeatureSearch(ShowcaseRepository showcases, JobListRepository jobLists, AdminAuditLog auditLog, Clock clock) {
        this.showcases = showcases;
        this.jobLists = jobLists;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public FeatureSearchResponse feature(FeatureSearchRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        Set<JobCode> jobs = request.jobCodes().stream().map(JobCode::of).collect(Collectors.toSet());
        Set<JobCode> offered = jobLists.find().map(JobList::offeredCodes).orElse(Set.of());
        Showcase showcase = showcases.find().orElseGet(Showcase::create);

        FeaturedSearch featured = showcase.feature(
                request.title(), PresetSearch.of(jobs, request.hours(), request.helpers()), request.position(), offered);

        showcases.save(showcase);
        admin.record(founder, "FEATURE_SEARCH", featured.id(), clock.instant());
        return new FeatureSearchResponse(featured.id(), featured.title(), showcase.positionOf(featured.id()));
    }
}
