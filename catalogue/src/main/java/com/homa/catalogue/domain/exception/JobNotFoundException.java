package com.homa.catalogue.domain.exception;

/** No job with this code is on the job list. */
public class JobNotFoundException extends CatalogueRuleViolationException {

    public JobNotFoundException(String code) {
        super("The job list has no job with code " + code);
    }
}
