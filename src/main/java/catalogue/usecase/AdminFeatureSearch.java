package catalogue.usecase;

import catalogue.domain.FeaturedSearch;
import catalogue.domain.Job;
import catalogue.domain.Showcase;
import catalogue.usecase.dto.FeatureSearchRequest;
import catalogue.usecase.dto.FeatureSearchResponse;
import catalogue.usecase.port.in.AdminSearchFeaturer;
import catalogue.usecase.port.out.JobRepository;
import catalogue.usecase.port.out.ShowcaseRepository;
import java.util.List;

public final class AdminFeatureSearch implements AdminSearchFeaturer {

    private final ShowcaseRepository showcases;
    private final JobRepository jobs;

    public AdminFeatureSearch(ShowcaseRepository showcases, JobRepository jobs) {
        this.showcases = showcases;
        this.jobs = jobs;
    }

    @Override
    public FeatureSearchResponse feature(FeatureSearchRequest request) {
        List<Job> searchedJobs = request.jobIds().stream().map(jobId -> Catalogue.job(jobs, jobId)).toList();
        Showcase showcase = showcases.get();
        FeaturedSearch search = showcase.feature(request.name(), searchedJobs, request.hours(), request.helpers());
        showcases.save(showcase);
        return new FeatureSearchResponse(search.id(), showcase.placeOf(search.id()));
    }
}
