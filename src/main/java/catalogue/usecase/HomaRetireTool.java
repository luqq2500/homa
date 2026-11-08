package catalogue.usecase;

import catalogue.domain.ToolCode;
import catalogue.domain.ToolList;
import catalogue.usecase.dto.RetireToolRequest;
import catalogue.usecase.dto.RetireToolResponse;
import catalogue.usecase.port.in.HomaToolRetirer;
import catalogue.usecase.port.out.CatalogueIEPublisher;
import catalogue.usecase.port.out.ToolListRepository;
import ie.events.ToolRetiredIE;
import java.time.Clock;
import java.time.Instant;

/** Retires a tool from Homa's tool list and tells providers they can no longer say they bring it. */
public final class HomaRetireTool implements HomaToolRetirer {

    private final ToolListRepository toolLists;
    private final CatalogueIEPublisher events;
    private final Clock clock;

    public HomaRetireTool(ToolListRepository toolLists, CatalogueIEPublisher events, Clock clock) {
        this.toolLists = toolLists;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public RetireToolResponse retire(RetireToolRequest request) {
        ToolList toolList = toolLists.load().orElseGet(ToolList::create);
        ToolCode code = ToolCode.of(request.code());
        toolList.retire(code);
        toolLists.save(toolList);
        Instant now = clock.instant();
        events.publish(new ToolRetiredIE(code.value(), now));
        return new RetireToolResponse(code.value(), now);
    }
}
