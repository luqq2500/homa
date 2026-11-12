package catalogue.usecase;

import catalogue.domain.ToolList;
import catalogue.usecase.dto.HomaViewToolListResponse;
import catalogue.usecase.port.in.HomaToolListViewer;
import catalogue.usecase.port.out.ToolListRepository;
import java.util.List;

/** Shows the founders the whole tool list, retired tools included, in the order the tools were added. */
public final class HomaViewToolList implements HomaToolListViewer {

    private final ToolListRepository toolLists;

    public HomaViewToolList(ToolListRepository toolLists) {
        this.toolLists = toolLists;
    }

    @Override
    public HomaViewToolListResponse view() {
        List<HomaViewToolListResponse.Tool> tools = toolLists.find().map(ToolList::tools).orElse(List.of()).stream()
                .map(tool -> new HomaViewToolListResponse.Tool(tool.code().value(), tool.name(), tool.isActive()))
                .toList();
        return new HomaViewToolListResponse(tools);
    }
}
