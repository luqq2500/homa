package catalogue.usecase;

import catalogue.domain.Job;
import catalogue.domain.JobCode;
import catalogue.domain.JobList;
import catalogue.usecase.dto.RenameJobRequest;
import catalogue.usecase.dto.RenameJobResponse;
import catalogue.usecase.port.in.HomaJobRenamer;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.port.out.JobListRepository;
import java.time.Clock;

/** Renames a job on Homa's job list. Its code stays, so no other context needs to know. */
public final class HomaRenameJob implements HomaJobRenamer {

    private final JobListRepository jobLists;
    private final AdminActions admin;
    private final Clock clock;

    public HomaRenameJob(JobListRepository jobLists, AdminAuditLog auditLog, Clock clock) {
        this.jobLists = jobLists;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public RenameJobResponse rename(RenameJobRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        JobCode code = JobCode.of(request.code());
        JobList jobList = jobLists.find().orElseGet(JobList::create);

        jobList.rename(code, request.name());
        Job renamed = jobList.job(code).orElseThrow();

        jobLists.save(jobList);
        admin.record(founder, "RENAME_JOB", code.value(), clock.instant());
        return new RenameJobResponse(renamed.code().value(), renamed.name());
    }
}
