package catalogue.usecase.dto;

import java.util.List;

public record ViewCatalogueResponse(List<Group> groups, List<Item> tools) {

    public ViewCatalogueResponse {
        groups = List.copyOf(groups);
        tools = List.copyOf(tools);
    }

    public record Group(String groupId, String name, List<Item> jobs) {

        public Group {
            jobs = List.copyOf(jobs);
        }
    }

    public record Item(String id, String name) {
    }
}
