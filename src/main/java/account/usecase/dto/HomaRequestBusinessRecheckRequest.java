package account.usecase.dto;

/** The founder, the business and why, for example after a safety-related report. */
public record HomaRequestBusinessRecheckRequest(String decidedBy, String providerId, String reason) {
}
