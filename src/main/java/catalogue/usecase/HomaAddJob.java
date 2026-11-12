package catalogue.usecase;

import catalogue.domain.Job;
import catalogue.domain.JobCode;
import catalogue.domain.JobGroup;
import catalogue.domain.JobList;
import catalogue.usecase.dto.AddJobRequest;
import catalogue.usecase.dto.AddJobResponse;
import catalogue.usecase.port.in.HomaJobAdder;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.port.out.CatalogueIEPublisher;
import catalogue.usecase.port.out.JobListRepository;
import ie.events.JobAddedIE;
import java.time.Clock;
import java.time.Instant;

/** Adds a job to Homa's job list and tells the other contexts, so providers can offer it. */
public final class HomaAddJob implements HomaJobAdder {

    private final JobListRepository jobLists;
    private final CatalogueIEPublisher events;
    private final AdminActions admin;
    private final Clock clock;

    public HomaAddJob(JobListRepository jobLists, CatalogueIEPublisher events, AdminAuditLog auditLog, Clock clock) {
        this.jobLists = jobLists;
        this.events = events;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public AddJobResponse add(AddJobRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        Instant now = clock.instant();
        JobCode code = JobCode.of(request.code());
        JobList jobList = jobLists.find().orElseGet(JobList::create);

        jobList.add(code, request.name(), JobGroup.of(request.group()));
        Job added = jobList.job(code).orElseThrow();

        jobLists.save(jobList);
        events.publish(new JobAddedIE(added.code().value(), added.name(), added.group().name(), now));
        admin.record(founder, "ADD_JOB", code.value(), now);
        return new AddJobResponse(added.code().value(), added.name(), added.group().name());
    }
}
