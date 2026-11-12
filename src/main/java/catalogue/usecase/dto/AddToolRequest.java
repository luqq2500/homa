package catalogue.usecase.dto;

/** What a founder gives to add a tool: its code and name. */
public record AddToolRequest(String decidedBy, String code, String name) {
}
