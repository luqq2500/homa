package offering.usecase.dto;

public record SetHelperRangeRequest(String providerId, int minHelpers, int maxHelpers) {}
