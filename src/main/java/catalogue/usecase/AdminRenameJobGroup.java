package catalogue.usecase;

import catalogue.domain.JobGroup;
import catalogue.usecase.dto.RenameJobGroupRequest;
import catalogue.usecase.dto.RenameJobGroupResponse;
import catalogue.usecase.port.in.AdminJobGroupRenamer;
import catalogue.usecase.port.out.JobGroupRepository;

public final class AdminRenameJobGroup implements AdminJobGroupRenamer {

    private final JobGroupRepository groups;

    public AdminRenameJobGroup(JobGroupRepository groups) {
        this.groups = groups;
    }

    @Override
    public RenameJobGroupResponse rename(RenameJobGroupRequest request) {
        JobGroup group = Catalogue.group(groups, request.groupId());
        group.rename(request.name());
        groups.save(group);
        return new RenameJobGroupResponse(group.id(), group.name());
    }
}
