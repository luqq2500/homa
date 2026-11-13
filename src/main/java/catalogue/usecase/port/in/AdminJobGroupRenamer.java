package catalogue.usecase.port.in;

import catalogue.usecase.dto.RenameJobGroupRequest;
import catalogue.usecase.dto.RenameJobGroupResponse;

/** An admin renames a job group. */
public interface AdminJobGroupRenamer {

    RenameJobGroupResponse rename(RenameJobGroupRequest request);
}
