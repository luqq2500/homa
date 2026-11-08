package catalogue.usecase.dto;

import java.util.Set;

/** Homa creates a homepage shelf driven by a preset search, at a place on the homepage from 1. */
public record CreateShelfRequest(String title, Set<String> jobCodes, int hours, int helpers, int order) {

    public CreateShelfRequest {
        jobCodes = Set.copyOf(jobCodes);
    }
}
