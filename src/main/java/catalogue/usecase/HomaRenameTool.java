package catalogue.usecase;

import catalogue.domain.Tool;
import catalogue.domain.ToolCode;
import catalogue.domain.ToolList;
import catalogue.usecase.dto.RenameToolRequest;
import catalogue.usecase.dto.RenameToolResponse;
import catalogue.usecase.port.in.HomaToolRenamer;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.port.out.ToolListRepository;
import java.time.Clock;

/** Renames a tool on Homa's tool list. Its code stays, so no other context needs to know. */
public final class HomaRenameTool implements HomaToolRenamer {

    private final ToolListRepository toolLists;
    private final AdminActions admin;
    private final Clock clock;

    public HomaRenameTool(ToolListRepository toolLists, AdminAuditLog auditLog, Clock clock) {
        this.toolLists = toolLists;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public RenameToolResponse rename(RenameToolRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        ToolCode code = ToolCode.of(request.code());
        ToolList toolList = toolLists.find().orElseGet(ToolList::create);

        toolList.rename(code, request.name());
        Tool renamed = toolList.tool(code).orElseThrow();

        toolLists.save(toolList);
        admin.record(founder, "RENAME_TOOL", code.value(), clock.instant());
        return new RenameToolResponse(renamed.code().value(), renamed.name());
    }
}
