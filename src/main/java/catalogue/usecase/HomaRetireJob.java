package catalogue.usecase;

import catalogue.domain.JobCode;
import catalogue.domain.JobList;
import catalogue.domain.Shelf;
import catalogue.usecase.dto.RetireJobRequest;
import catalogue.usecase.dto.RetireJobResponse;
import catalogue.usecase.port.in.HomaJobRetirer;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.port.out.CatalogueIEPublisher;
import catalogue.usecase.port.out.JobListRepository;
import catalogue.usecase.port.out.ShelfRepository;
import ie.events.JobRetiredIE;
import java.time.Clock;
import java.time.Instant;

/**
 * Retires a job from Homa's job list and tells the other contexts, so it comes off every listing that has
 * it. It also comes off every shelf's search here; a shelf left with no job is no longer shown. Retiring
 * changes providers' listings, so it needs a reason.
 */
public final class HomaRetireJob implements HomaJobRetirer {

    private final JobListRepository jobLists;
    private final ShelfRepository shelves;
    private final CatalogueIEPublisher events;
    private final AdminActions admin;
    private final Clock clock;

    public HomaRetireJob(
            JobListRepository jobLists, ShelfRepository shelves, CatalogueIEPublisher events, AdminAuditLog auditLog, Clock clock) {
        this.jobLists = jobLists;
        this.shelves = shelves;
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
        withdrawFromShelves(code);
        events.publish(new JobRetiredIE(code.value(), now));
        admin.record(founder, "RETIRE_JOB", code.value(), reason, now);
        return new RetireJobResponse(code.value(), now);
    }

    private void withdrawFromShelves(JobCode retired) {
        for (Shelf shelf : shelves.findAll()) {
            if (shelf.withdrawJob(retired)) {
                shelves.save(shelf);
            }
        }
    }
}
