package catalogue.usecase;

import catalogue.domain.JobList;
import catalogue.usecase.dto.HomaViewJobListResponse;
import catalogue.usecase.port.in.HomaJobListViewer;
import catalogue.usecase.port.out.JobListRepository;
import java.util.List;

/** Shows the founders the whole job list, retired jobs included, in the order the jobs were added. */
public final class HomaViewJobList implements HomaJobListViewer {

    private final JobListRepository jobLists;

    public HomaViewJobList(JobListRepository jobLists) {
        this.jobLists = jobLists;
    }

    @Override
    public HomaViewJobListResponse view() {
        List<HomaViewJobListResponse.Job> jobs = jobLists.find().map(JobList::jobs).orElse(List.of()).stream()
                .map(job -> new HomaViewJobListResponse.Job(job.code().value(), job.name(), job.group().name(), job.isActive()))
                .toList();
        return new HomaViewJobListResponse(jobs);
    }
}
