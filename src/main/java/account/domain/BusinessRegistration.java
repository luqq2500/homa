package account.domain;

import java.time.Instant;
import java.util.Objects;

/** A business provider's SSM registration, and when Homa checked it. */
public record BusinessRegistration(String ssmNumber, Instant checkedAt) {

    public BusinessRegistration {
        ssmNumber = AccountText.require(ssmNumber, "An SSM registration number");
        Objects.requireNonNull(checkedAt);
    }
}
