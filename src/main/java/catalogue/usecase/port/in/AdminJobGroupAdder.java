package catalogue.usecase.port.in;

import catalogue.usecase.dto.AddJobGroupRequest;
import catalogue.usecase.dto.AddJobGroupResponse;

/** An admin adds a group to the job list, such as kitchen or living room. */
public interface AdminJobGroupAdder {

    AddJobGroupResponse add(AddJobGroupRequest request);
}
