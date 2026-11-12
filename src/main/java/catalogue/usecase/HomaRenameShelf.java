package catalogue.usecase;

import catalogue.domain.Shelf;
import catalogue.usecase.dto.RenameShelfRequest;
import catalogue.usecase.dto.RenameShelfResponse;
import catalogue.usecase.exception.ShelfNotFoundException;
import catalogue.usecase.port.in.HomaShelfRenamer;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.port.out.ShelfRepository;
import java.time.Clock;

/**
 * Gives a shelf a new title. Its search and place stay. The same title changes nothing, so nothing is
 * saved or recorded.
 */
public final class HomaRenameShelf implements HomaShelfRenamer {

    private final ShelfRepository shelves;
    private final AdminActions admin;
    private final Clock clock;

    public HomaRenameShelf(ShelfRepository shelves, AdminAuditLog auditLog, Clock clock) {
        this.shelves = shelves;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public RenameShelfResponse rename(RenameShelfRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        Shelf shelf = shelves.find(request.shelfId()).orElseThrow(() -> new ShelfNotFoundException(request.shelfId()));

        if (shelf.rename(request.title())) {
            shelves.save(shelf);
            admin.record(founder, "RENAME_SHELF", shelf.id(), clock.instant());
        }
        return new RenameShelfResponse(shelf.id(), shelf.title());
    }
}
