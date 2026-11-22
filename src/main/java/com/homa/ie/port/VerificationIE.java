package com.homa.ie.port;

/**
 * Every integration event the Verification context publishes. The context's single IE publisher takes this
 * interface. Verification events name a person by personId only, never by the identity document number (IDN-3).
 */
public interface VerificationIE extends IntegrationEvent {
}
