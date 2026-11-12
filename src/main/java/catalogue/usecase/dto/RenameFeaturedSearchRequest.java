package catalogue.usecase.dto;

/** What a founder gives to give a featured search a new title. */
public record RenameFeaturedSearchRequest(String decidedBy, String featuredSearchId, String title) {
}
