package com.homa.customer.usecase.dto;

/**
 * A registration. {@code issuer} and {@code subject} name the identity-provider account the person signed in with
 * (CUS-7); infra/security fills them from the verified token, never from the form. No phone number is asked for
 * (CUS-1).
 *
 * @param issuer               the identity provider, such as https://accounts.google.com
 * @param subject              the account's id at that identity provider
 * @param name                 the name the customer goes by
 * @param streetAddress        the home address
 * @param city                 the Homa city code of the home address, such as SALAK_TINGGI
 * @param arrivalNote          how the team reaches the home
 * @param termsOfServiceAgreed whether the customer ticked the Terms of Service (CUS-2)
 */
public record RegisterAccountRequest(
        String issuer,
        String subject,
        String name,
        String streetAddress,
        String city,
        String arrivalNote,
        boolean termsOfServiceAgreed) {
}
