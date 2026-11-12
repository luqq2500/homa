package catalogue.usecase.port.in;

import catalogue.usecase.dto.MoveFeaturedSearchRequest;
import catalogue.usecase.dto.MoveFeaturedSearchResponse;

/** Homa's founders move a featured search to another place in the showcase. */
public interface HomaFeaturedSearchMover {

    MoveFeaturedSearchResponse move(MoveFeaturedSearchRequest request);
}
