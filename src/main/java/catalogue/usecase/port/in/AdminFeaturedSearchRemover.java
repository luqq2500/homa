package catalogue.usecase.port.in;

import catalogue.usecase.dto.RemoveFeaturedSearchRequest;
import catalogue.usecase.dto.RemoveFeaturedSearchResponse;

/** An admin removes a featured search from the showcase. */
public interface AdminFeaturedSearchRemover {

    RemoveFeaturedSearchResponse remove(RemoveFeaturedSearchRequest request);
}
