package catalogue.usecase.dto;

/** What a founder gives to remove a shelf. */
public record RemoveShelfRequest(String decidedBy, String shelfId) {
}
