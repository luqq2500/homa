package com.homa.rating.usecase.port.in;

import com.homa.rating.usecase.dto.SubmitRatingRequest;
import com.homa.rating.usecase.dto.SubmitRatingResponse;

/** RTG-1: the customer rates the provider and the provider rates the customer. */
public interface RatingSubmitter {

    SubmitRatingResponse submit(SubmitRatingRequest request);
}
