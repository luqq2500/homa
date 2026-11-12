package catalogue.usecase.dto;

import java.util.List;

/** Every featured search left in the showcase, in order. */
public record RemoveFeaturedSearchResponse(List<String> featuredSearchIds) {

    public RemoveFeaturedSearchResponse {
        featuredSearchIds = List.copyOf(featuredSearchIds);
    }
}
