package com.homa.provider.domain.exception;

/** A Provider business rule (PRV-*, HLP-*) refused an act. Each subclass names the rule it guards. */
public abstract class ProviderRuleViolationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected ProviderRuleViolationException(String message) {
        super(message);
    }
}
