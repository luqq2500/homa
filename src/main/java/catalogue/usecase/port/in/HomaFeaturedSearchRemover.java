package catalogue.usecase.port.in;

import catalogue.usecase.dto.RemoveFeaturedSearchRequest;
import catalogue.usecase.dto.RemoveFeaturedSearchResponse;

/** Homa's founders take a featured search off the showcase. */
public interface HomaFeaturedSearchRemover {

    RemoveFeaturedSearchResponse remove(RemoveFeaturedSearchRequest request);
}
