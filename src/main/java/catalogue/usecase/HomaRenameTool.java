package catalogue.usecase;

import catalogue.domain.Tool;
import catalogue.domain.ToolCode;
import catalogue.domain.ToolList;
import catalogue.usecase.dto.RenameToolRequest;
import catalogue.usecase.dto.RenameToolResponse;
import catalogue.usecase.port.in.HomaToolRenamer;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.port.out.CatalogueIEPublisher;
import catalogue.usecase.port.out.ToolListRepository;
import ie.events.ToolRenamedIE;
import java.time.Clock;
import java.time.Instant;

/**
 * Renames a tool on Homa's tool list. Its code stays, so listings and reservations keep it; the other
 * contexts are told so the name they show is updated.
 */
public final class HomaRenameTool implements HomaToolRenamer {

    private final ToolListRepository toolLists;
    private final CatalogueIEPublisher events;
    private final AdminActions admin;
    private final Clock clock;

    public HomaRenameTool(ToolListRepository toolLists, CatalogueIEPublisher events, AdminAuditLog auditLog, Clock clock) {
        this.toolLists = toolLists;
        this.events = events;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public RenameToolResponse rename(RenameToolRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        Instant now = clock.instant();
        ToolCode code = ToolCode.of(request.code());
        ToolList toolList = toolLists.find().orElseGet(ToolList::create);

        toolList.rename(code, request.name());
        Tool renamed = toolList.tool(code).orElseThrow();

        toolLists.save(toolList);
        events.publish(new ToolRenamedIE(renamed.code().value(), renamed.name(), now));
        admin.record(founder, "RENAME_TOOL", code.value(), now);
        return new RenameToolResponse(renamed.code().value(), renamed.name());
    }
}
