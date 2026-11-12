package catalogue.usecase;

import catalogue.domain.JobCode;
import catalogue.domain.JobList;
import catalogue.usecase.dto.RetireJobRequest;
import catalogue.usecase.dto.RetireJobResponse;
import catalogue.usecase.port.in.HomaJobRetirer;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.port.out.CatalogueIEPublisher;
import catalogue.usecase.port.out.JobListRepository;
import ie.events.JobRetiredIE;
import java.time.Clock;
import java.time.Instant;

/**
 * Retires a job from Homa's job list and tells the other contexts, so it comes off every listing that has
 * it. Retiring changes providers' listings, so it needs a reason.
 */
public final class HomaRetireJob implements HomaJobRetirer {

    private final JobListRepository jobLists;
    private final CatalogueIEPublisher events;
    private final AdminActions admin;
    private final Clock clock;

    public HomaRetireJob(JobListRepository jobLists, CatalogueIEPublisher events, AdminAuditLog auditLog, Clock clock) {
        this.jobLists = jobLists;
        this.events = events;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public RetireJobResponse retire(RetireJobRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        String reason = AdminActions.reason(request.reason(), "retire a job");
        Instant now = clock.instant();
        JobCode code = JobCode.of(request.code());
        JobList jobList = jobLists.find().orElseGet(JobList::create);

        jobList.retire(code);

        jobLists.save(jobList);
        events.publish(new JobRetiredIE(code.value(), now));
        admin.record(founder, "RETIRE_JOB", code.value(), reason, now);
        return new RetireJobResponse(code.value(), now);
    }
}
