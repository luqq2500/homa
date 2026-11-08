package offering.domain;

/** A tool from Homa's tool list, by its code, such as {@code STEAM_MOP}. */
public record ToolCode(String value) {

    public ToolCode {
        value = OfferingText.require(value, "A tool code");
    }

    public static ToolCode of(String value) {
        return new ToolCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
