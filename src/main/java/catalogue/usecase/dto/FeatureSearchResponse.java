package catalogue.usecase.dto;

/** The featured search as it was featured, and its place in the showcase. */
public record FeatureSearchResponse(String featuredSearchId, String title, int position) {
}
