package offering.usecase.dto;

import kernel.Money;

public record SetTravelFeeRequest(String providerId, boolean separate, Money amount) {}
