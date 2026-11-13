package catalogue.usecase.port.in;

import catalogue.usecase.dto.RenameFeaturedSearchRequest;
import catalogue.usecase.dto.RenameFeaturedSearchResponse;

/** An admin renames a featured search. */
public interface AdminFeaturedSearchRenamer {

    RenameFeaturedSearchResponse rename(RenameFeaturedSearchRequest request);
}
