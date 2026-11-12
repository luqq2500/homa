package account.usecase.dto;

/**
 * The provider's confirmation for a verified, named person.
 *
 * @param providerId the business, from authentication
 * @param verificationId the verified person's verification
 * @param lawfullyEmployed whether the provider ticked that this named person is lawfully employed by or
 *     engaged with them and allowed to work in Malaysia; the person joins only with it
 * @param termsVersion the version of the terms shown with the tick
 */
public record ConfirmTeamMemberRequest(String providerId, String verificationId, boolean lawfullyEmployed, String termsVersion) {
}
