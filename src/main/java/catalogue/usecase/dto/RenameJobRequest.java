package catalogue.usecase.dto;

/** What a founder gives to rename a job. */
public record RenameJobRequest(String decidedBy, String code, String name) {
}
