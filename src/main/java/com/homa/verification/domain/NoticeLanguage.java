package com.homa.verification.domain;

import java.util.Objects;

/**
 * A language Homa's privacy notice is offered in (DAT-11): its language tag, which picks the notice's text, and
 * its name as the rules write it, such as "Bahasa Malaysia".
 */
public record NoticeLanguage(String tag, String name) {

    public NoticeLanguage {
        Objects.requireNonNull(tag, "tag");
        Objects.requireNonNull(name, "name");
    }
}
