package catalogue.domain;

/** Identifies a job on Homa's job list, such as {@code KITCHEN_GREASE}. Providers and search match on it. */
public record JobCode(String value) {

    public JobCode {
        CatalogueText.requireCode(value);
    }

    public static JobCode of(String value) {
        return new JobCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
