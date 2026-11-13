package catalogue.usecase;

import catalogue.domain.Job;
import catalogue.domain.JobGroup;
import catalogue.usecase.dto.MoveJobRequest;
import catalogue.usecase.dto.MoveJobResponse;
import catalogue.usecase.port.in.AdminJobMover;
import catalogue.usecase.port.out.JobGroupRepository;
import catalogue.usecase.port.out.JobRepository;

public final class AdminMoveJob implements AdminJobMover {

    private final JobRepository jobs;
    private final JobGroupRepository groups;

    public AdminMoveJob(JobRepository jobs, JobGroupRepository groups) {
        this.jobs = jobs;
        this.groups = groups;
    }

    @Override
    public MoveJobResponse move(MoveJobRequest request) {
        Job job = Catalogue.job(jobs, request.jobId());
        JobGroup group = Catalogue.group(groups, request.groupId());
        job.moveTo(group);
        jobs.save(job);
        return new MoveJobResponse(job.id(), job.groupId());
    }
}
