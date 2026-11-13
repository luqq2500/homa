package offering.support;

import java.util.List;
import java.util.Optional;
import offering.domain.Offering;
import offering.usecase.port.out.OfferingRepository;
import support.InMemoryStore;

public final class InMemoryOfferingRepository extends InMemoryStore<Offering> implements OfferingRepository {

    public InMemoryOfferingRepository() {
        super(Offering::providerId);
    }

    @Override
    public List<Offering> offeringJob(String jobId) {
        return all().stream().filter(offering -> offering.jobIds().contains(jobId)).toList();
    }

    @Override
    public List<Offering> bringingTool(String toolId) {
        return all().stream().filter(offering -> offering.toolIds().contains(toolId)).toList();
    }
}
