package catalogue.usecase.dto;

/** What a founder gives to move a shelf. */
public record MoveShelfRequest(String decidedBy, String shelfId, int position) {
}
