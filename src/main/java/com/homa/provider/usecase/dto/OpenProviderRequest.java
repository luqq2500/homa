package com.homa.provider.usecase.dto;

/**
 * A verified person opens a provider account as its owner (PRV-1).
 *
 * @param actingPersonId the person opening it, as Verification knows them; {@code infra/security} fills it from the
 *                       token
 */
public record OpenProviderRequest(String actingPersonId) {
}
