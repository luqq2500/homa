package discovery.domain;

import discovery.domain.exception.DiscoveryDetailRequiredException;

/** Text in a search is required and kept without surrounding spaces. */
final class DiscoveryText {

    private DiscoveryText() {
    }

    static String require(String text, String what) {
        if (text == null || text.isBlank()) {
            throw new DiscoveryDetailRequiredException(what);
        }
        return text.strip();
    }
}
