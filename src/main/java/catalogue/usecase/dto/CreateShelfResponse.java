package catalogue.usecase.dto;

/** The shelf as created. */
public record CreateShelfResponse(String shelfId, String title, int order) {
}
