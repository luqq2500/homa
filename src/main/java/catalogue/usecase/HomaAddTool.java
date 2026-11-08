package catalogue.usecase;

import catalogue.domain.ToolCode;
import catalogue.domain.ToolList;
import catalogue.usecase.dto.AddToolRequest;
import catalogue.usecase.dto.AddToolResponse;
import catalogue.usecase.port.in.HomaToolAdder;
import catalogue.usecase.port.out.CatalogueIEPublisher;
import catalogue.usecase.port.out.ToolListRepository;
import ie.events.ToolAddedIE;
import java.time.Clock;

/** Adds a tool to Homa's tool list, starting the list with its first tool, and tells providers they can say they bring it. */
public final class HomaAddTool implements HomaToolAdder {

    private final ToolListRepository toolLists;
    private final CatalogueIEPublisher events;
    private final Clock clock;

    public HomaAddTool(ToolListRepository toolLists, CatalogueIEPublisher events, Clock clock) {
        this.toolLists = toolLists;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public AddToolResponse add(AddToolRequest request) {
        ToolList toolList = toolLists.load().orElseGet(ToolList::create);
        ToolCode code = ToolCode.of(request.code());
        toolList.add(code, request.name());
        toolLists.save(toolList);
        var tool = toolList.tool(code).orElseThrow();
        events.publish(new ToolAddedIE(code.value(), tool.name(), clock.instant()));
        return new AddToolResponse(code.value(), tool.name());
    }
}
