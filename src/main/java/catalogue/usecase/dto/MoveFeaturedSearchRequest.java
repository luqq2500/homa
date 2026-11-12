package catalogue.usecase.dto;

/** What a founder gives to move a featured search: its new place in the showcase, from 1. */
public record MoveFeaturedSearchRequest(String decidedBy, String featuredSearchId, int position) {
}
