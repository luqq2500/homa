package reservation.domain;

import java.util.UUID;

/** Type-safe generator for Enquiry ids. */
public final class EnquiryId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
