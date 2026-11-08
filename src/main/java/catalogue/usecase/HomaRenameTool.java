package catalogue.usecase;

import catalogue.domain.ToolCode;
import catalogue.domain.ToolList;
import catalogue.usecase.dto.RenameToolRequest;
import catalogue.usecase.dto.RenameToolResponse;
import catalogue.usecase.port.in.HomaToolRenamer;
import catalogue.usecase.port.out.ToolListRepository;

/** Renames a tool on Homa's tool list. Its code stays the same. */
public final class HomaRenameTool implements HomaToolRenamer {

    private final ToolListRepository toolLists;

    public HomaRenameTool(ToolListRepository toolLists) {
        this.toolLists = toolLists;
    }

    @Override
    public RenameToolResponse rename(RenameToolRequest request) {
        ToolList toolList = toolLists.load().orElseGet(ToolList::create);
        ToolCode code = ToolCode.of(request.code());
        toolList.rename(code, request.name());
        toolLists.save(toolList);
        return new RenameToolResponse(code.value(), toolList.tool(code).orElseThrow().name());
    }
}
