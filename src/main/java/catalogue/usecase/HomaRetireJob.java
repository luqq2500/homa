package catalogue.usecase;

import catalogue.domain.JobCode;
import catalogue.domain.JobList;
import catalogue.usecase.dto.RetireJobRequest;
import catalogue.usecase.dto.RetireJobResponse;
import catalogue.usecase.port.in.HomaJobRetirer;
import catalogue.usecase.port.out.CatalogueIEPublisher;
import catalogue.usecase.port.out.JobListRepository;
import ie.events.JobRetiredIE;
import java.time.Clock;
import java.time.Instant;

/** Retires a job from Homa's job list and tells providers it can no longer be offered. */
public final class HomaRetireJob implements HomaJobRetirer {

    private final JobListRepository jobLists;
    private final CatalogueIEPublisher events;
    private final Clock clock;

    public HomaRetireJob(JobListRepository jobLists, CatalogueIEPublisher events, Clock clock) {
        this.jobLists = jobLists;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public RetireJobResponse retire(RetireJobRequest request) {
        JobList jobList = jobLists.load().orElseGet(JobList::create);
        JobCode code = JobCode.of(request.code());
        jobList.retire(code);
        jobLists.save(jobList);
        Instant now = clock.instant();
        events.publish(new JobRetiredIE(code.value(), now));
        return new RetireJobResponse(code.value(), now);
    }
}
