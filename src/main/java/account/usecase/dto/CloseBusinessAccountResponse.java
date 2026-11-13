package account.usecase.dto;

import java.time.Instant;

public record CloseBusinessAccountResponse(String businessId, Instant closedAt) {}
