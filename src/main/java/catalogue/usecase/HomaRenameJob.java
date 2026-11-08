package catalogue.usecase;

import catalogue.domain.JobCode;
import catalogue.domain.JobList;
import catalogue.usecase.dto.RenameJobRequest;
import catalogue.usecase.dto.RenameJobResponse;
import catalogue.usecase.port.in.HomaJobRenamer;
import catalogue.usecase.port.out.JobListRepository;

/** Renames a job on Homa's job list. Its code, which providers and search use, stays the same. */
public final class HomaRenameJob implements HomaJobRenamer {

    private final JobListRepository jobLists;

    public HomaRenameJob(JobListRepository jobLists) {
        this.jobLists = jobLists;
    }

    @Override
    public RenameJobResponse rename(RenameJobRequest request) {
        JobList jobList = jobLists.load().orElseGet(JobList::create);
        JobCode code = JobCode.of(request.code());
        jobList.rename(code, request.name());
        jobLists.save(jobList);
        return new RenameJobResponse(code.value(), jobList.job(code).orElseThrow().name());
    }
}
