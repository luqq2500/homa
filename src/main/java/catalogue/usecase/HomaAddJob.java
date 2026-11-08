package catalogue.usecase;

import catalogue.domain.JobCode;
import catalogue.domain.JobGroup;
import catalogue.domain.JobList;
import catalogue.usecase.dto.AddJobRequest;
import catalogue.usecase.dto.AddJobResponse;
import catalogue.usecase.port.in.HomaJobAdder;
import catalogue.usecase.port.out.CatalogueIEPublisher;
import catalogue.usecase.port.out.JobListRepository;
import ie.events.JobAddedIE;
import java.time.Clock;

/** Adds a job to Homa's job list, starting the list with its first job, and tells providers it can be offered. */
public final class HomaAddJob implements HomaJobAdder {

    private final JobListRepository jobLists;
    private final CatalogueIEPublisher events;
    private final Clock clock;

    public HomaAddJob(JobListRepository jobLists, CatalogueIEPublisher events, Clock clock) {
        this.jobLists = jobLists;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public AddJobResponse add(AddJobRequest request) {
        JobList jobList = jobLists.load().orElseGet(JobList::create);
        JobCode code = JobCode.of(request.code());
        jobList.add(code, request.name(), JobGroup.of(request.group()));
        jobLists.save(jobList);
        var job = jobList.job(code).orElseThrow();
        events.publish(new JobAddedIE(code.value(), job.name(), job.group().name(), clock.instant()));
        return new AddJobResponse(code.value(), job.name(), job.group().name());
    }
}
