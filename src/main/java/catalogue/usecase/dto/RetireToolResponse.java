package catalogue.usecase.dto;

import java.time.Instant;

/** The retired tool, and when it was retired. */
public record RetireToolResponse(String code, Instant retiredAt) {
}
