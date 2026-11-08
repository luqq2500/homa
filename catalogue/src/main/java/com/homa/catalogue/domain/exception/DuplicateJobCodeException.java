package com.homa.catalogue.domain.exception;

/** Job codes are unique, including retired jobs, which are never deleted. */
public class DuplicateJobCodeException extends CatalogueRuleViolationException {

    public DuplicateJobCodeException(String code) {
        super("The job list already has a job with code " + code);
    }
}
