package com.homa.provider.usecase.port.out;

/** Ids for the integration events Provider publishes. */
public interface EventIds {

    /** A new id, unique per event. */
    String next();
}
