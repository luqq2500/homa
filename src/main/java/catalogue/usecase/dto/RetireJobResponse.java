package catalogue.usecase.dto;

import java.time.Instant;

/** The retired job and when it was retired. */
public record RetireJobResponse(String code, Instant retiredAt) {
}
