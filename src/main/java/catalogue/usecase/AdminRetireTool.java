package catalogue.usecase;

import catalogue.domain.Tool;
import catalogue.usecase.dto.RetireToolRequest;
import catalogue.usecase.dto.RetireToolResponse;
import catalogue.usecase.port.in.AdminToolRetirer;
import catalogue.usecase.port.out.CatalogueIEPublisher;
import catalogue.usecase.port.out.ToolRepository;
import ie.events.ToolRetiredIE;
import java.time.Clock;
import java.time.Instant;

public final class AdminRetireTool implements AdminToolRetirer {

    private final ToolRepository tools;
    private final CatalogueIEPublisher publisher;
    private final Clock clock;

    public AdminRetireTool(ToolRepository tools, CatalogueIEPublisher publisher, Clock clock) {
        this.tools = tools;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public RetireToolResponse retire(RetireToolRequest request) {
        Instant now = clock.instant();
        Tool tool = Catalogue.tool(tools, request.toolId());
        tool.retire(request.reason(), now);
        tools.save(tool);
        publisher.publish(new ToolRetiredIE(tool.id(), request.reason(), now));
        return new RetireToolResponse(tool.id(), now);
    }
}
