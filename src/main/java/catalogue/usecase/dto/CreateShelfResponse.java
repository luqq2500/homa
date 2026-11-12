package catalogue.usecase.dto;

/** The shelf as it was created. */
public record CreateShelfResponse(String shelfId, String title, int order) {
}
