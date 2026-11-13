package verification.usecase.dto;

import java.time.Instant;
import java.util.List;

public record ViewVerificationResponse(String businessStatus, String registeredName, Instant verifiedUntil, Instant lastVerifiedAt, List<Person> people) {

    public ViewVerificationResponse {
        people = List.copyOf(people);
    }

    public record Person(String memberId, String status, String role, String registeredName, Instant verifiedUntil,
                         Instant lastVerifiedAt) {
    }
}
