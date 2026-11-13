package account.usecase.dto;

import java.time.Instant;

public record CloseCustomerAccountResponse(String customerId, Instant closedAt) {}
