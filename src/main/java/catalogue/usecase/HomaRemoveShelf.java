package catalogue.usecase;

import catalogue.domain.Homepage;
import catalogue.domain.Shelf;
import catalogue.usecase.dto.RemoveShelfRequest;
import catalogue.usecase.dto.RemoveShelfResponse;
import catalogue.usecase.exception.ShelfNotFoundException;
import catalogue.usecase.port.in.HomaShelfRemover;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.port.out.HomepageRepository;
import catalogue.usecase.port.out.ShelfRepository;
import java.time.Clock;

/**
 * Takes a shelf off the homepage and deletes it; the shelves after it move up one.
 */
public final class HomaRemoveShelf implements HomaShelfRemover {

    private final ShelfRepository shelves;
    private final HomepageRepository homepages;
    private final AdminActions admin;
    private final Clock clock;

    public HomaRemoveShelf(ShelfRepository shelves, HomepageRepository homepages, AdminAuditLog auditLog, Clock clock) {
        this.shelves = shelves;
        this.homepages = homepages;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public RemoveShelfResponse remove(RemoveShelfRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        Shelf shelf = shelves.find(request.shelfId()).orElseThrow(() -> new ShelfNotFoundException(request.shelfId()));
        Homepage homepage = homepages.find().orElseThrow(() -> new ShelfNotFoundException(request.shelfId()));

        homepage.remove(shelf.id());

        homepages.save(homepage);
        shelves.remove(shelf.id());
        admin.record(founder, "REMOVE_SHELF", shelf.id(), clock.instant());
        return new RemoveShelfResponse(homepage.shelfIds());
    }
}
