package catalogue.usecase;

import catalogue.domain.Tool;
import catalogue.domain.ToolList;
import catalogue.usecase.dto.ProviderViewToolListResponse;
import catalogue.usecase.port.in.ProviderToolListViewer;
import catalogue.usecase.port.out.ToolListRepository;
import java.util.List;

/** Shows a provider the tools they can say they bring now. Retired tools are left out. */
public final class ProviderViewToolList implements ProviderToolListViewer {

    private final ToolListRepository toolLists;

    public ProviderViewToolList(ToolListRepository toolLists) {
        this.toolLists = toolLists;
    }

    @Override
    public ProviderViewToolListResponse view() {
        List<ProviderViewToolListResponse.Tool> tools = toolLists.find().map(ToolList::tools).orElse(List.of()).stream()
                .filter(Tool::isActive)
                .map(tool -> new ProviderViewToolListResponse.Tool(tool.code().value(), tool.name()))
                .toList();
        return new ProviderViewToolListResponse(tools);
    }
}
