package offering.domain;

import java.util.HashSet;
import java.util.Set;
import offering.domain.exception.JobNotOfferableException;
import offering.domain.exception.ToolNotPickableException;

/** The jobs and tools on Homa's lists that providers can pick right now: added and not retired. */
public final class OfferableCatalogue {

    private final Set<String> jobIds;
    private final Set<String> toolIds;

    public OfferableCatalogue(Set<String> jobIds, Set<String> toolIds) {
        this.jobIds = new HashSet<>(jobIds);
        this.toolIds = new HashSet<>(toolIds);
    }

    public static OfferableCatalogue empty() {
        return new OfferableCatalogue(Set.of(), Set.of());
    }

    public void addJob(String jobId) {
        jobIds.add(jobId);
    }

    public void retireJob(String jobId) {
        jobIds.remove(jobId);
    }

    public void addTool(String toolId) {
        toolIds.add(toolId);
    }

    public void retireTool(String toolId) {
        toolIds.remove(toolId);
    }

    void requireOfferable(Set<String> chosenJobIds) {
        for (String jobId : chosenJobIds) {
            if (!jobIds.contains(jobId)) {
                throw new JobNotOfferableException(jobId);
            }
        }
    }

    void requirePickable(Set<String> chosenToolIds) {
        for (String toolId : chosenToolIds) {
            if (!toolIds.contains(toolId)) {
                throw new ToolNotPickableException(toolId);
            }
        }
    }

    public Set<String> jobIds() {
        return Set.copyOf(jobIds);
    }

    public Set<String> toolIds() {
        return Set.copyOf(toolIds);
    }
}
