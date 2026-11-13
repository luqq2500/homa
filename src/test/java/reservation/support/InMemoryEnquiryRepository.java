package reservation.support;

import reservation.domain.Enquiry;
import reservation.usecase.port.out.EnquiryRepository;
import support.InMemoryStore;

public final class InMemoryEnquiryRepository extends InMemoryStore<Enquiry> implements EnquiryRepository {

    public InMemoryEnquiryRepository() {
        super(Enquiry::id);
    }
}
