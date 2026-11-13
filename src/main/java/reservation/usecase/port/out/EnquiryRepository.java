package reservation.usecase.port.out;

import java.util.List;
import java.util.Optional;
import reservation.domain.Enquiry;

public interface EnquiryRepository {

    void save(Enquiry enquiry);

    Optional<Enquiry> find(String id);

    List<Enquiry> all();
}
