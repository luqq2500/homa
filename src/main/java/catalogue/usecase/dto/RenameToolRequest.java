package catalogue.usecase.dto;

/** What a founder gives to rename a tool. */
public record RenameToolRequest(String decidedBy, String code, String name) {
}
