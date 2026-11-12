package account.usecase.dto;

/** The verification waiting for the person, and the address of the KYC provider's check to open on the provider's phone. */
public record StartAddingTeamMemberResponse(String verificationId, String checkUrl) {
}
