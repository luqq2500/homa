package catalogue.usecase.port.in;

import catalogue.usecase.dto.RenameFeaturedSearchRequest;
import catalogue.usecase.dto.RenameFeaturedSearchResponse;

/** Homa's founders give a featured search a new title. */
public interface HomaFeaturedSearchRenamer {

    RenameFeaturedSearchResponse rename(RenameFeaturedSearchRequest request);
}
