package com.homa.customer.domain.exception;

/** CUS-4: Support suspends a customer for a reason, such as a confirmed Safety &amp; Misconduct report. */
public class MissingSuspensionReasonException extends RuntimeException {

    public MissingSuspensionReasonException() {
        super("CUS-4: Support must give the reason for suspending a customer");
    }
}
