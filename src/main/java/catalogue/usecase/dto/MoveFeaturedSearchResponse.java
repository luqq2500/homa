package catalogue.usecase.dto;

import java.util.List;

/** Every featured search in the showcase, in its new order. */
public record MoveFeaturedSearchResponse(List<String> featuredSearchIds) {

    public MoveFeaturedSearchResponse {
        featuredSearchIds = List.copyOf(featuredSearchIds);
    }
}
