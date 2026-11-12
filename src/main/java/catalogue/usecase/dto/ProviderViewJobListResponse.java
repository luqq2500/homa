package catalogue.usecase.dto;

import java.util.List;

/** The jobs a provider can offer now, by display group, in the order the groups and jobs were added. */
public record ProviderViewJobListResponse(List<Group> groups) {

    public ProviderViewJobListResponse {
        groups = List.copyOf(groups);
    }

    /** A display group, such as Kitchen, with its jobs. */
    public record Group(String name, List<Job> jobs) {

        public Group {
            jobs = List.copyOf(jobs);
        }
    }

    /** One job a provider can offer. */
    public record Job(String code, String name) {
    }
}
