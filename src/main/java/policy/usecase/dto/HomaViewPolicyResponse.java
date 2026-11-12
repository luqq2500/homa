package policy.usecase.dto;

import java.time.Instant;
import java.util.List;

/** The Homa-set values in force: the version, from when, and every value in the order the spec lists them. */
public record HomaViewPolicyResponse(int version, Instant effectiveFrom, List<Value> values) {

    public HomaViewPolicyResponse {
        values = List.copyOf(values);
    }

    /**
     * One value.
     *
     * @param name the name to change it by, such as {@code SERVICE_FEE}
     * @param kind how its amount is written: {@code PERCENTAGE}, {@code PERIOD}, {@code COUNT} or {@code RANGE}
     * @param amount the amount in force, such as {@code 15%}, {@code PT2H}, {@code 300} or {@code 1-6}
     */
    public record Value(String name, String kind, String amount) {
    }
}
