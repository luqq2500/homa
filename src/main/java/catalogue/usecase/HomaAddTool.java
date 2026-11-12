package catalogue.usecase;

import catalogue.domain.Tool;
import catalogue.domain.ToolCode;
import catalogue.domain.ToolList;
import catalogue.usecase.dto.AddToolRequest;
import catalogue.usecase.dto.AddToolResponse;
import catalogue.usecase.port.in.HomaToolAdder;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.port.out.CatalogueIEPublisher;
import catalogue.usecase.port.out.ToolListRepository;
import ie.events.ToolAddedIE;
import java.time.Clock;
import java.time.Instant;

/** Adds a tool to Homa's tool list and tells the other contexts, so providers can say they bring it. */
public final class HomaAddTool implements HomaToolAdder {

    private final ToolListRepository toolLists;
    private final CatalogueIEPublisher events;
    private final AdminActions admin;
    private final Clock clock;

    public HomaAddTool(ToolListRepository toolLists, CatalogueIEPublisher events, AdminAuditLog auditLog, Clock clock) {
        this.toolLists = toolLists;
        this.events = events;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public AddToolResponse add(AddToolRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        Instant now = clock.instant();
        ToolCode code = ToolCode.of(request.code());
        ToolList toolList = toolLists.find().orElseGet(ToolList::create);

        toolList.add(code, request.name());
        Tool added = toolList.tool(code).orElseThrow();

        toolLists.save(toolList);
        events.publish(new ToolAddedIE(added.code().value(), added.name(), now));
        admin.record(founder, "ADD_TOOL", code.value(), now);
        return new AddToolResponse(added.code().value(), added.name());
    }
}
