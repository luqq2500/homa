package catalogue.usecase.dto;

import java.util.List;

/** Every tool on the tool list, retired ones included, in the order they were added. */
public record HomaViewToolListResponse(List<Tool> tools) {

    public HomaViewToolListResponse {
        tools = List.copyOf(tools);
    }

    /** One tool, and whether providers can still say they bring it. */
    public record Tool(String code, String name, boolean active) {
    }
}
