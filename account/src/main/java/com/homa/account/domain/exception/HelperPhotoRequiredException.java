package com.homa.account.domain.exception;

/** Customers see each helper's photo, so a helper needs one. */
public class HelperPhotoRequiredException extends AccountRuleViolationException {

    public HelperPhotoRequiredException() {
        super("A helper needs a photo");
    }
}
