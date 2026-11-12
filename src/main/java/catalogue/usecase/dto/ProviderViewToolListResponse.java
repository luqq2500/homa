package catalogue.usecase.dto;

import java.util.List;

/** The tools a provider can say they bring now, in the order they were added. */
public record ProviderViewToolListResponse(List<Tool> tools) {

    public ProviderViewToolListResponse {
        tools = List.copyOf(tools);
    }

    /** One tool a provider can say they bring. */
    public record Tool(String code, String name) {
    }
}
