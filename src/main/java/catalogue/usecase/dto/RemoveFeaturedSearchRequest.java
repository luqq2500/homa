package catalogue.usecase.dto;

/** What a founder gives to take a featured search off the showcase. */
public record RemoveFeaturedSearchRequest(String decidedBy, String featuredSearchId) {
}
