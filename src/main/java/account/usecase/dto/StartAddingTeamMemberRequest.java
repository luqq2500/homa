package account.usecase.dto;

/**
 * The add form.
 *
 * @param providerId the business, from authentication
 * @param role {@code HELPER} or {@code SUPERVISOR}
 * @param photo the stored profile photo's reference
 * @param lawfullyEmployed whether the provider ticked that this person is lawfully employed by or engaged
 *     with them and allowed to work in Malaysia; the form cannot be sent without it
 * @param termsVersion the version of the terms shown with the tick
 */
public record StartAddingTeamMemberRequest(
        String providerId, String role, String photo, boolean lawfullyEmployed, String termsVersion) {
}
