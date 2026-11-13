package reservation.usecase.dto;

public record ChangeAccessNotesRequest(String customerId, String reservationId, String accessNotes) {}
