package catalogue.usecase.dto;

/** Homa adds a job to its job list, in a display group such as Kitchen. */
public record AddJobRequest(String code, String name, String group) {
}
