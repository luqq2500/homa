package catalogue.usecase;

import catalogue.domain.Tool;
import catalogue.domain.ToolList;
import catalogue.usecase.dto.ViewToolListRequest;
import catalogue.usecase.dto.ViewToolListResponse;
import catalogue.usecase.port.in.ProviderToolListViewer;
import catalogue.usecase.port.out.ToolListRepository;
import java.util.List;

/** Shows the tools a provider can say they bring now. Retired tools are left out. */
public final class ProviderViewToolList implements ProviderToolListViewer {

    private final ToolListRepository toolLists;

    public ProviderViewToolList(ToolListRepository toolLists) {
        this.toolLists = toolLists;
    }

    @Override
    public ViewToolListResponse view(ViewToolListRequest request) {
        return new ViewToolListResponse(toolLists.load().map(ToolList::tools).orElse(List.of()).stream()
                .filter(Tool::isActive)
                .map(tool -> new ViewToolListResponse.Tool(tool.code().value(), tool.name()))
                .toList());
    }
}
