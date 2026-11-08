package catalogue.usecase.dto;

import java.util.List;

/** The tools providers can pick now, in the order Homa added them. */
public record ViewToolListResponse(List<Tool> tools) {

    public ViewToolListResponse {
        tools = List.copyOf(tools);
    }

    public record Tool(String code, String name) {
    }
}
