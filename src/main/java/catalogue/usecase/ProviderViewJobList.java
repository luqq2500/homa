package catalogue.usecase;

import catalogue.domain.Job;
import catalogue.domain.JobList;
import catalogue.usecase.dto.ProviderViewJobListResponse;
import catalogue.usecase.port.in.ProviderJobListViewer;
import catalogue.usecase.port.out.JobListRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Shows a provider the jobs they can offer now, by display group. Retired jobs are left out. */
public final class ProviderViewJobList implements ProviderJobListViewer {

    private final JobListRepository jobLists;

    public ProviderViewJobList(JobListRepository jobLists) {
        this.jobLists = jobLists;
    }

    @Override
    public ProviderViewJobListResponse view() {
        Map<String, List<ProviderViewJobListResponse.Job>> byGroup = jobLists.find().map(JobList::jobs).orElse(List.of()).stream()
                .filter(Job::isActive)
                .collect(Collectors.groupingBy(
                        job -> job.group().name(),
                        LinkedHashMap::new,
                        Collectors.mapping(job -> new ProviderViewJobListResponse.Job(job.code().value(), job.name()), Collectors.toList())));
        List<ProviderViewJobListResponse.Group> groups = byGroup.entrySet().stream()
                .map(group -> new ProviderViewJobListResponse.Group(group.getKey(), group.getValue()))
                .toList();
        return new ProviderViewJobListResponse(groups);
    }
}
