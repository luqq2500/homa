package catalogue.domain;

import catalogue.domain.exception.CatalogueNameRequiredException;
import catalogue.domain.exception.InvalidCatalogueCodeException;
import java.util.regex.Pattern;

/** The text rules shared by codes and names on Homa's lists. */
final class CatalogueText {

    private static final Pattern CODE_SHAPE = Pattern.compile("[A-Z][A-Z0-9]*(_[A-Z0-9]+)*");

    private CatalogueText() {
    }

    /** Codes are upper-case words joined by underscores, such as {@code KITCHEN_GREASE}. */
    static void requireCode(String code) {
        if (code == null || !CODE_SHAPE.matcher(code).matches()) {
            throw new InvalidCatalogueCodeException(code);
        }
    }

    /** Names are required and kept without surrounding spaces. */
    static String requireName(String name, String what) {
        if (name == null || name.isBlank()) {
            throw new CatalogueNameRequiredException(what);
        }
        return name.strip();
    }
}
