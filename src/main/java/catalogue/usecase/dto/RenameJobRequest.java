package catalogue.usecase.dto;

/** Homa renames a job; its code stays the same. */
public record RenameJobRequest(String code, String name) {
}
