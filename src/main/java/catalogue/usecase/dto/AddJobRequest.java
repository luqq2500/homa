package catalogue.usecase.dto;

/** What a founder gives to add a job: its code, name and display group. */
public record AddJobRequest(String decidedBy, String code, String name, String group) {
}
