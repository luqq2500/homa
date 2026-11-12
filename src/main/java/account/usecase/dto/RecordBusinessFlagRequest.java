package account.usecase.dto;

/** The business and the flag, such as being struck off or a sanctions match. */
public record RecordBusinessFlagRequest(String providerId, String reason) {
}
