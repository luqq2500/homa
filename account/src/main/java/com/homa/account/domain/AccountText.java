package com.homa.account.domain;

import com.homa.account.domain.exception.AccountDetailRequiredException;

/** The rule shared by every piece of text on an account: it is required, and kept without surrounding spaces. */
final class AccountText {

    private AccountText() {
    }

    static String require(String text, String what) {
        if (text == null || text.isBlank()) {
            throw new AccountDetailRequiredException(what);
        }
        return text.strip();
    }
}
