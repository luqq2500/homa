package account.domain;

import account.domain.exception.AccessNotesTooLongException;

/**
 * Notes that help the team get in, such as the unit, guardhouse registration and parking: up to 500
 * characters, kept without surrounding spaces. Blank means none.
 */
public record AccessNotes(String text) {

    public static final int MAX_LENGTH = 500;
    public static final AccessNotes NONE = new AccessNotes("");

    public AccessNotes {
        text = text == null ? "" : text.strip();
        if (text.length() > MAX_LENGTH) {
            throw new AccessNotesTooLongException(text.length(), MAX_LENGTH);
        }
    }

    public static AccessNotes of(String text) {
        return new AccessNotes(text);
    }

    public boolean isNone() {
        return text.isEmpty();
    }
}
