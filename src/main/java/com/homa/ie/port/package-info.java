/**
 * Integration event contracts. {@code IntegrationEvent} is what every event shares; one interface per publishing
 * context extends it: CustomerIE, ProviderIE, ListingIE, ReservationIE, PaymentIE, SupportIE and RatingIE. A
 * context's single IE publisher takes its interface. {@code RelayedIE} is the envelope a consuming context receives
 * from the outbox. Contracts only: no behaviour, and nothing but com.homa.kernel and the JDK may be imported
 * (ArchitectureTest rule 2).
 */
package com.homa.ie.port;
