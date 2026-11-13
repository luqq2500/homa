package catalogue.domain;

import catalogue.domain.exception.RetirementReasonMissingException;
import java.time.Instant;

/** Why and when a job or tool was retired. Retiring changes providers' listings, so it needs a reason. */
public record Retirement(String reason, Instant retiredAt) {

    public Retirement {
        if (reason == null || reason.isBlank()) {
            throw new RetirementReasonMissingException();
        }
    }
}
