package catalogue.usecase;

import catalogue.domain.Tool;
import catalogue.usecase.dto.AddToolRequest;
import catalogue.usecase.dto.AddToolResponse;
import catalogue.usecase.port.in.AdminToolAdder;
import catalogue.usecase.port.out.CatalogueIEPublisher;
import catalogue.usecase.port.out.ToolRepository;
import ie.events.ToolAddedIE;
import java.time.Clock;

public final class AdminAddTool implements AdminToolAdder {

    private final ToolRepository tools;
    private final CatalogueIEPublisher publisher;
    private final Clock clock;

    public AdminAddTool(ToolRepository tools, CatalogueIEPublisher publisher, Clock clock) {
        this.tools = tools;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public AddToolResponse add(AddToolRequest request) {
        Tool tool = Tool.add(request.name());
        tools.save(tool);
        publisher.publish(new ToolAddedIE(tool.id(), tool.name(), clock.instant()));
        return new AddToolResponse(tool.id());
    }
}
