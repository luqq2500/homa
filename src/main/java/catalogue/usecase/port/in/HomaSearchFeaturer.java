package catalogue.usecase.port.in;

import catalogue.usecase.dto.FeatureSearchRequest;
import catalogue.usecase.dto.FeatureSearchResponse;

/** Homa's founders feature a search on the homepage. */
public interface HomaSearchFeaturer {

    FeatureSearchResponse feature(FeatureSearchRequest request);
}
