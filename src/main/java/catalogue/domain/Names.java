package catalogue.domain;

import catalogue.domain.exception.NameMissingException;

/** The rule every name in the catalogue follows. */
final class Names {

    private Names() {
    }

    static String required(String name) {
        if (name == null || name.isBlank()) {
            throw new NameMissingException();
        }
        return name.strip();
    }
}
