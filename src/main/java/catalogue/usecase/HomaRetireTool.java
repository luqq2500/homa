package catalogue.usecase;

import catalogue.domain.ToolCode;
import catalogue.domain.ToolList;
import catalogue.usecase.dto.RetireToolRequest;
import catalogue.usecase.dto.RetireToolResponse;
import catalogue.usecase.port.in.HomaToolRetirer;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.port.out.CatalogueIEPublisher;
import catalogue.usecase.port.out.ToolListRepository;
import ie.events.ToolRetiredIE;
import java.time.Clock;
import java.time.Instant;

/**
 * Retires a tool from Homa's tool list and tells the other contexts, so it comes off every listing that has
 * it. Retiring changes providers' listings, so it needs a reason.
 */
public final class HomaRetireTool implements HomaToolRetirer {

    private final ToolListRepository toolLists;
    private final CatalogueIEPublisher events;
    private final AdminActions admin;
    private final Clock clock;

    public HomaRetireTool(ToolListRepository toolLists, CatalogueIEPublisher events, AdminAuditLog auditLog, Clock clock) {
        this.toolLists = toolLists;
        this.events = events;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public RetireToolResponse retire(RetireToolRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        String reason = AdminActions.reason(request.reason(), "retire a tool");
        Instant now = clock.instant();
        ToolCode code = ToolCode.of(request.code());
        ToolList toolList = toolLists.find().orElseGet(ToolList::create);

        toolList.retire(code);

        toolLists.save(toolList);
        events.publish(new ToolRetiredIE(code.value(), now));
        admin.record(founder, "RETIRE_TOOL", code.value(), reason, now);
        return new RetireToolResponse(code.value(), now);
    }
}
