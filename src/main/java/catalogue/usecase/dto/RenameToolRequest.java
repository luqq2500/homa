package catalogue.usecase.dto;

/** Homa renames a tool; its code stays the same. */
public record RenameToolRequest(String code, String name) {
}
