package policy.usecase.dto;

/**
 * What a founder gives to change one Homa-set value.
 *
 * @param value which value, as {@link HomaViewPolicyResponse} names it, such as {@code SERVICE_FEE}
 * @param amount the new amount, written as {@link HomaViewPolicyResponse} shows its kind: a percentage
 *     such as {@code 17.5%}, a period of whole minutes such as {@code PT2H} or {@code P30D}, a count such as {@code 300},
 *     or a range of hours such as {@code 1-6}
 * @param reason why; a change applies to every new reservation, so it needs one
 */
public record ChangePolicyValueRequest(String decidedBy, String value, String amount, String reason) {
}
