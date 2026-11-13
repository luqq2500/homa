package catalogue.usecase;

import catalogue.domain.Job;
import catalogue.domain.JobGroup;
import catalogue.domain.Tool;
import catalogue.usecase.exception.JobGroupNotFoundException;
import catalogue.usecase.exception.JobNotFoundException;
import catalogue.usecase.exception.ToolNotFoundException;
import catalogue.usecase.port.out.JobGroupRepository;
import catalogue.usecase.port.out.JobRepository;
import catalogue.usecase.port.out.ToolRepository;

/** Looks up what a usecase is about, or fails because it is not on Homa's lists. */
final class Catalogue {

    private Catalogue() {
    }

    static JobGroup group(JobGroupRepository groups, String groupId) {
        return groups.find(groupId).orElseThrow(() -> new JobGroupNotFoundException(groupId));
    }

    static Job job(JobRepository jobs, String jobId) {
        return jobs.find(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
    }

    static Tool tool(ToolRepository tools, String toolId) {
        return tools.find(toolId).orElseThrow(() -> new ToolNotFoundException(toolId));
    }
}
