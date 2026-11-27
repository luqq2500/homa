package com.homa.customer.usecase.exception;

/** The signed-in customer asked for something only another customer may do, such as changing that one's profile. */
public class NotAllowedException extends RuntimeException {

    public NotAllowedException(String message) {
        super(message);
    }
}
