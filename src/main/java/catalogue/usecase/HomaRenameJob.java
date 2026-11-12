package catalogue.usecase;

import catalogue.domain.Job;
import catalogue.domain.JobCode;
import catalogue.domain.JobList;
import catalogue.usecase.dto.RenameJobRequest;
import catalogue.usecase.dto.RenameJobResponse;
import catalogue.usecase.port.in.HomaJobRenamer;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.port.out.CatalogueIEPublisher;
import catalogue.usecase.port.out.JobListRepository;
import ie.events.JobRenamedIE;
import java.time.Clock;
import java.time.Instant;

/**
 * Renames a job on Homa's job list. Its code stays, so listings and reservations keep it; the other
 * contexts are told so the name they show is updated. Renaming to the same name changes nothing, so
 * nothing is saved, told or recorded.
 */
public final class HomaRenameJob implements HomaJobRenamer {

    private final JobListRepository jobLists;
    private final CatalogueIEPublisher events;
    private final AdminActions admin;
    private final Clock clock;

    public HomaRenameJob(JobListRepository jobLists, CatalogueIEPublisher events, AdminAuditLog auditLog, Clock clock) {
        this.jobLists = jobLists;
        this.events = events;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public RenameJobResponse rename(RenameJobRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        Instant now = clock.instant();
        JobCode code = JobCode.of(request.code());
        JobList jobList = jobLists.find().orElseGet(JobList::create);

        boolean changed = jobList.rename(code, request.name());
        Job renamed = jobList.job(code).orElseThrow();

        if (changed) {
            jobLists.save(jobList);
            events.publish(new JobRenamedIE(renamed.code().value(), renamed.name(), now));
            admin.record(founder, "RENAME_JOB", code.value(), now);
        }
        return new RenameJobResponse(renamed.code().value(), renamed.name());
    }
}
