package discovery.domain;

/** A job from Homa's job list, by its code, such as {@code KITCHEN}. */
public record JobCode(String value) {

    public JobCode {
        value = DiscoveryText.require(value, "A job code");
    }

    public static JobCode of(String value) {
        return new JobCode(value);
    }
}
