package feedback.domain;

import feedback.domain.exception.FeedbackDetailRequiredException;

/** Ids on ratings are required. */
final class FeedbackText {

    private FeedbackText() {
    }

    static String require(String text, String what) {
        if (text == null || text.isBlank()) {
            throw new FeedbackDetailRequiredException(what);
        }
        return text.strip();
    }
}
