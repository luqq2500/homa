package catalogue.usecase.port.in;

import catalogue.usecase.dto.MoveFeaturedSearchRequest;
import catalogue.usecase.dto.MoveFeaturedSearchResponse;

/** An admin moves a featured search to another place in the showcase. */
public interface AdminFeaturedSearchMover {

    MoveFeaturedSearchResponse move(MoveFeaturedSearchRequest request);
}
