package catalogue.usecase;

import catalogue.domain.Homepage;
import catalogue.domain.JobCode;
import catalogue.domain.JobList;
import catalogue.domain.PresetSearch;
import catalogue.domain.Shelf;
import catalogue.usecase.dto.CreateShelfRequest;
import catalogue.usecase.dto.CreateShelfResponse;
import catalogue.usecase.port.in.HomaShelfCreator;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.port.out.HomepageRepository;
import catalogue.usecase.port.out.JobListRepository;
import catalogue.usecase.port.out.ShelfRepository;
import java.time.Clock;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates a homepage shelf with its preset search, for jobs providers can offer now, and places it on the
 * homepage; the shelves from that place on move down one.
 */
public final class HomaCreateShelf implements HomaShelfCreator {

    private final ShelfRepository shelves;
    private final HomepageRepository homepages;
    private final JobListRepository jobLists;
    private final AdminActions admin;
    private final Clock clock;

    public HomaCreateShelf(
            ShelfRepository shelves, HomepageRepository homepages, JobListRepository jobLists, AdminAuditLog auditLog, Clock clock) {
        this.shelves = shelves;
        this.homepages = homepages;
        this.jobLists = jobLists;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public CreateShelfResponse create(CreateShelfRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        Set<JobCode> jobs = request.jobCodes().stream().map(JobCode::of).collect(Collectors.toSet());
        Set<JobCode> offered = jobLists.find().map(JobList::offeredCodes).orElse(Set.of());
        Homepage homepage = homepages.find().orElseGet(Homepage::create);

        Shelf shelf = Shelf.create(request.title(), PresetSearch.of(jobs, request.hours(), request.helpers()), offered);
        homepage.place(shelf.id(), request.order());

        shelves.save(shelf);
        homepages.save(homepage);
        admin.record(founder, "CREATE_SHELF", shelf.id(), clock.instant());
        return new CreateShelfResponse(shelf.id(), shelf.title(), homepage.positionOf(shelf.id()));
    }
}
