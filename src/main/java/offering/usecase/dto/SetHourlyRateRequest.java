package offering.usecase.dto;

import kernel.Money;

public record SetHourlyRateRequest(String providerId, Money hourlyRate) {}
