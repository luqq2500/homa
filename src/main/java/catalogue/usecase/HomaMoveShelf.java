package catalogue.usecase;

import catalogue.domain.Homepage;
import catalogue.usecase.dto.MoveShelfRequest;
import catalogue.usecase.dto.MoveShelfResponse;
import catalogue.usecase.exception.ShelfNotFoundException;
import catalogue.usecase.port.in.HomaShelfMover;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.port.out.HomepageRepository;
import java.time.Clock;

/**
 * Moves a shelf to another place on the homepage; the shelves in between move along. Its own place
 * changes nothing, so nothing is saved or recorded.
 */
public final class HomaMoveShelf implements HomaShelfMover {

    private final HomepageRepository homepages;
    private final AdminActions admin;
    private final Clock clock;

    public HomaMoveShelf(HomepageRepository homepages, AdminAuditLog auditLog, Clock clock) {
        this.homepages = homepages;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public MoveShelfResponse move(MoveShelfRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        Homepage homepage = homepages.find().orElseThrow(() -> new ShelfNotFoundException(request.shelfId()));

        if (homepage.move(request.shelfId(), request.position())) {
            homepages.save(homepage);
            admin.record(founder, "MOVE_SHELF", request.shelfId(), clock.instant());
        }
        return new MoveShelfResponse(homepage.shelfIds());
    }
}
