package com.homa.listing.usecase.port.in;

import com.homa.listing.usecase.dto.SearchProvidersByCityRequest;
import com.homa.listing.usecase.dto.SearchProvidersByCityResponse;

/** A customer finds the providers covering their city (RES-1). */
public interface ProviderSearcher {

    SearchProvidersByCityResponse search(SearchProvidersByCityRequest request);
}
