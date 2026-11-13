package catalogue.usecase;

import catalogue.domain.Job;
import catalogue.domain.JobGroup;
import catalogue.usecase.dto.AddJobRequest;
import catalogue.usecase.dto.AddJobResponse;
import catalogue.usecase.port.in.AdminJobAdder;
import catalogue.usecase.port.out.CatalogueIEPublisher;
import catalogue.usecase.port.out.JobGroupRepository;
import catalogue.usecase.port.out.JobRepository;
import ie.events.JobAddedIE;
import java.time.Clock;

public final class AdminAddJob implements AdminJobAdder {

    private final JobRepository jobs;
    private final JobGroupRepository groups;
    private final CatalogueIEPublisher publisher;
    private final Clock clock;

    public AdminAddJob(JobRepository jobs, JobGroupRepository groups, CatalogueIEPublisher publisher, Clock clock) {
        this.jobs = jobs;
        this.groups = groups;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public AddJobResponse add(AddJobRequest request) {
        JobGroup group = Catalogue.group(groups, request.groupId());
        Job job = Job.add(request.name(), group);
        jobs.save(job);
        publisher.publish(new JobAddedIE(job.id(), job.name(), job.groupId(), clock.instant()));
        return new AddJobResponse(job.id());
    }
}
