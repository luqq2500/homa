package account.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * The provider's confirmation, ticked on the add form for one person, that this person is lawfully employed
 * by or engaged with them and allowed to work in Malaysia, and that they are responsible for it. Homa does
 * not check work permits.
 */
public record EmploymentConfirmation(Instant givenAt, String termsVersion) {

    public EmploymentConfirmation {
        Objects.requireNonNull(givenAt);
        termsVersion = AccountText.require(termsVersion, "The terms version");
    }

    public static EmploymentConfirmation given(String termsVersion, Instant now) {
        return new EmploymentConfirmation(now, termsVersion);
    }
}
