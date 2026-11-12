package catalogue.usecase.dto;

import java.util.Set;

/** What a founder gives to create a homepage shelf: its title, its preset search and its place on the homepage, from 1. */
public record CreateShelfRequest(String decidedBy, String title, Set<String> jobCodes, int hours, int helpers, int order) {

    public CreateShelfRequest {
        jobCodes = Set.copyOf(jobCodes);
    }
}
