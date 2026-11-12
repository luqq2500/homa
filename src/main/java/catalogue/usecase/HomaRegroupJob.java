package catalogue.usecase;

import catalogue.domain.Job;
import catalogue.domain.JobCode;
import catalogue.domain.JobGroup;
import catalogue.domain.JobList;
import catalogue.usecase.dto.RegroupJobRequest;
import catalogue.usecase.dto.RegroupJobResponse;
import catalogue.usecase.port.in.HomaJobRegrouper;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.port.out.CatalogueIEPublisher;
import catalogue.usecase.port.out.JobListRepository;
import ie.events.JobRegroupedIE;
import java.time.Clock;
import java.time.Instant;

/**
 * Moves a job to another display group, such as from Kitchen to Living room. Its code stays, so listings
 * and reservations keep it; the other contexts are told so the group they show is updated.
 */
public final class HomaRegroupJob implements HomaJobRegrouper {

    private final JobListRepository jobLists;
    private final CatalogueIEPublisher events;
    private final AdminActions admin;
    private final Clock clock;

    public HomaRegroupJob(JobListRepository jobLists, CatalogueIEPublisher events, AdminAuditLog auditLog, Clock clock) {
        this.jobLists = jobLists;
        this.events = events;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public RegroupJobResponse regroup(RegroupJobRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        Instant now = clock.instant();
        JobCode code = JobCode.of(request.code());
        JobList jobList = jobLists.find().orElseGet(JobList::create);

        jobList.regroup(code, JobGroup.of(request.group()));
        Job moved = jobList.job(code).orElseThrow();

        jobLists.save(jobList);
        events.publish(new JobRegroupedIE(moved.code().value(), moved.group().name(), now));
        admin.record(founder, "REGROUP_JOB", code.value(), now);
        return new RegroupJobResponse(moved.code().value(), moved.group().name());
    }
}
