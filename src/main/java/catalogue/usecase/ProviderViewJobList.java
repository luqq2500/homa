package catalogue.usecase;

import catalogue.domain.Job;
import catalogue.domain.JobList;
import catalogue.usecase.dto.ViewJobListRequest;
import catalogue.usecase.dto.ViewJobListResponse;
import catalogue.usecase.port.in.ProviderJobListViewer;
import catalogue.usecase.port.out.JobListRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Shows the jobs a provider can offer now, grouped as Homa groups them. Retired jobs are left out. */
public final class ProviderViewJobList implements ProviderJobListViewer {

    private final JobListRepository jobLists;

    public ProviderViewJobList(JobListRepository jobLists) {
        this.jobLists = jobLists;
    }

    @Override
    public ViewJobListResponse view(ViewJobListRequest request) {
        List<Job> offered = jobLists.load().map(JobList::jobs).orElse(List.of()).stream().filter(Job::isActive).toList();
        Map<String, List<ViewJobListResponse.Job>> byGroup = offered.stream().collect(Collectors.groupingBy(
                job -> job.group().name(),
                LinkedHashMap::new,
                Collectors.mapping(job -> new ViewJobListResponse.Job(job.code().value(), job.name()), Collectors.toList())));
        return new ViewJobListResponse(byGroup.entrySet().stream()
                .map(group -> new ViewJobListResponse.Group(group.getKey(), group.getValue()))
                .toList());
    }
}
