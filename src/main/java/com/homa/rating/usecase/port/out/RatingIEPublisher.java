package com.homa.rating.usecase.port.out;

import com.homa.ie.port.RatingIE;

/** Rating's single integration event publisher, called inside the use case's transaction (ADR 0002). */
public interface RatingIEPublisher {

    void publish(RatingIE event);
}
