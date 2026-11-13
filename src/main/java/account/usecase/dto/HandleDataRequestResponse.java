package account.usecase.dto;

import java.time.Instant;

public record HandleDataRequestResponse(String requestId, Instant handledAt) {}
