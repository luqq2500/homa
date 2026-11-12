package catalogue.usecase.dto;

/** What a founder gives to retire a job, with the reason. */
public record RetireJobRequest(String decidedBy, String code, String reason) {
}
