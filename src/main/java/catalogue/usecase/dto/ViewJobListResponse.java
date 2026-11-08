package catalogue.usecase.dto;

import java.util.List;

/** The jobs providers can offer now, grouped for scanning, groups and jobs in the order Homa added them. */
public record ViewJobListResponse(List<Group> groups) {

    public ViewJobListResponse {
        groups = List.copyOf(groups);
    }

    public record Group(String name, List<Job> jobs) {
        public Group {
            jobs = List.copyOf(jobs);
        }
    }

    public record Job(String code, String name) {
    }
}
