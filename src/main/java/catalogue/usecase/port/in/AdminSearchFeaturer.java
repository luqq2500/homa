package catalogue.usecase.port.in;

import catalogue.usecase.dto.FeatureSearchRequest;
import catalogue.usecase.dto.FeatureSearchResponse;

/** An admin features a search in the homepage showcase. */
public interface AdminSearchFeaturer {

    FeatureSearchResponse feature(FeatureSearchRequest request);
}
