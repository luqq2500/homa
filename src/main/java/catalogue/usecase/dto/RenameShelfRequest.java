package catalogue.usecase.dto;

/** What a founder gives to rename a shelf. */
public record RenameShelfRequest(String decidedBy, String shelfId, String title) {
}
