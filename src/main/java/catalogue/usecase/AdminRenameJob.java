package catalogue.usecase;

import catalogue.domain.Job;
import catalogue.usecase.dto.RenameJobRequest;
import catalogue.usecase.dto.RenameJobResponse;
import catalogue.usecase.port.in.AdminJobRenamer;
import catalogue.usecase.port.out.JobRepository;

public final class AdminRenameJob implements AdminJobRenamer {

    private final JobRepository jobs;

    public AdminRenameJob(JobRepository jobs) {
        this.jobs = jobs;
    }

    @Override
    public RenameJobResponse rename(RenameJobRequest request) {
        Job job = Catalogue.job(jobs, request.jobId());
        job.rename(request.name());
        jobs.save(job);
        return new RenameJobResponse(job.id(), job.name());
    }
}
