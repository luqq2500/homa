package com.homa.reservation.usecase.port.out;

/** A new, unique integration event id for each event the context publishes. */
public interface EventIds {

    String next();
}
