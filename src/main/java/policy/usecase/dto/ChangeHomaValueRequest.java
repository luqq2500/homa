package policy.usecase.dto;

/** {@code amount} is in the value's own unit: percent, minutes, hours, metres or a count. */
public record ChangeHomaValueRequest(String adminId, String value, long amount, String reason) {
}
