package com.homa.ie.events;

/**
 * Test only: a record that sits in the events package and looks like an event by name but is not an
 * IntegrationEvent. The outbox must refuse to read a row of this type.
 */
public record NotAnEventIE(String eventId) {
}
