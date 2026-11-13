package catalogue.usecase;

import catalogue.domain.JobGroup;
import catalogue.usecase.dto.AddJobGroupRequest;
import catalogue.usecase.dto.AddJobGroupResponse;
import catalogue.usecase.port.in.AdminJobGroupAdder;
import catalogue.usecase.port.out.JobGroupRepository;

public final class AdminAddJobGroup implements AdminJobGroupAdder {

    private final JobGroupRepository groups;

    public AdminAddJobGroup(JobGroupRepository groups) {
        this.groups = groups;
    }

    @Override
    public AddJobGroupResponse add(AddJobGroupRequest request) {
        JobGroup group = JobGroup.add(request.name());
        groups.save(group);
        return new AddJobGroupResponse(group.id());
    }
}
