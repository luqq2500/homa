package com.homa.verification.domain;

/** Whether a person needs a work permit to work in the country, as the country policy sets it (Definitions, §14). */
public enum PersonStatus {
    /** Can work in the country without a work permit. */
    LOCAL,
    /** Needs a work permit to work in the country. */
    FOREIGN
}
