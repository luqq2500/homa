package reservation.domain;

import java.util.UUID;

/** Generates the id of an enquiry. */
public final class EnquiryId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
