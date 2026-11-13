package offering.usecase.dto;

public record SetHoursRequest(String providerId, int minHours, int maxHours) {}
