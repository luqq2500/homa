package offering.usecase.port.out;

import java.util.List;
import java.util.Optional;
import offering.domain.Offering;

public interface OfferingRepository {

    void save(Offering offering);

    Optional<Offering> find(String id);

    List<Offering> all();

    List<Offering> offeringJob(String jobId);

    List<Offering> bringingTool(String toolId);
}
