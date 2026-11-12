package catalogue.usecase.dto;

import java.util.List;

/** Every job on the job list, retired ones included, in the order they were added. */
public record HomaViewJobListResponse(List<Job> jobs) {

    public HomaViewJobListResponse {
        jobs = List.copyOf(jobs);
    }

    /** One job, and whether providers can still offer it. */
    public record Job(String code, String name, String group, boolean active) {
    }
}
