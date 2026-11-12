package catalogue.usecase.dto;

/** What a founder gives to retire a tool, with the reason. */
public record RetireToolRequest(String decidedBy, String code, String reason) {
}
