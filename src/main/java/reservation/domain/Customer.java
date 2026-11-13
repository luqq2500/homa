package reservation.domain;

/** The customer as a reservation needs them: their name, their current address and their account's access notes. */
public record Customer(String customerId, String name, Address address, String accessNotes) {
}
