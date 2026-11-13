package catalogue.usecase;

import catalogue.domain.Tool;
import catalogue.usecase.dto.RenameToolRequest;
import catalogue.usecase.dto.RenameToolResponse;
import catalogue.usecase.port.in.AdminToolRenamer;
import catalogue.usecase.port.out.ToolRepository;

public final class AdminRenameTool implements AdminToolRenamer {

    private final ToolRepository tools;

    public AdminRenameTool(ToolRepository tools) {
        this.tools = tools;
    }

    @Override
    public RenameToolResponse rename(RenameToolRequest request) {
        Tool tool = Catalogue.tool(tools, request.toolId());
        tool.rename(request.name());
        tools.save(tool);
        return new RenameToolResponse(tool.id(), tool.name());
    }
}
