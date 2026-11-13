package catalogue.usecase;

import catalogue.domain.Job;
import catalogue.domain.Showcase;
import catalogue.usecase.dto.RetireJobRequest;
import catalogue.usecase.dto.RetireJobResponse;
import catalogue.usecase.port.in.AdminJobRetirer;
import catalogue.usecase.port.out.CatalogueIEPublisher;
import catalogue.usecase.port.out.JobRepository;
import catalogue.usecase.port.out.ShowcaseRepository;
import ie.events.JobRetiredIE;
import java.time.Clock;
import java.time.Instant;

public final class AdminRetireJob implements AdminJobRetirer {

    private final JobRepository jobs;
    private final ShowcaseRepository showcases;
    private final CatalogueIEPublisher publisher;
    private final Clock clock;

    public AdminRetireJob(JobRepository jobs, ShowcaseRepository showcases, CatalogueIEPublisher publisher,
                          Clock clock) {
        this.jobs = jobs;
        this.showcases = showcases;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public RetireJobResponse retire(RetireJobRequest request) {
        Instant now = clock.instant();
        Job job = Catalogue.job(jobs, request.jobId());
        job.retire(request.reason(), now);
        Showcase showcase = showcases.get();
        showcase.retireJob(job.id());

        jobs.save(job);
        showcases.save(showcase);
        publisher.publish(new JobRetiredIE(job.id(), request.reason(), now));
        return new RetireJobResponse(job.id(), now);
    }
}
