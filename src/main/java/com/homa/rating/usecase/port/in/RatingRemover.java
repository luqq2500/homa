package com.homa.rating.usecase.port.in;

import com.homa.rating.usecase.dto.RemoveRatingRequest;
import com.homa.rating.usecase.dto.RemoveRatingResponse;

/** RTG-11: Support removes a rating with abusive content or personal contact or address details. */
public interface RatingRemover {

    RemoveRatingResponse remove(RemoveRatingRequest request);
}
