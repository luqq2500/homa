package catalogue.usecase.dto;

/** What a founder gives to move a job to another display group. */
public record RegroupJobRequest(String decidedBy, String code, String group) {
}
