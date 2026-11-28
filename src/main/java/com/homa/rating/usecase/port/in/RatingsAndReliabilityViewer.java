package com.homa.rating.usecase.port.in;

import com.homa.rating.usecase.dto.ViewRatingsAndReliabilityRequest;
import com.homa.rating.usecase.dto.ViewRatingsAndReliabilityResponse;

/** RTG-6, RTG-10: who sees a party's ratings and reliability counts, and what they see. */
public interface RatingsAndReliabilityViewer {

    ViewRatingsAndReliabilityResponse view(ViewRatingsAndReliabilityRequest request);
}
