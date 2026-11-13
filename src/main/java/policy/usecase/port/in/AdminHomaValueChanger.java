package policy.usecase.port.in;

import policy.usecase.dto.ChangeHomaValueRequest;
import policy.usecase.dto.ChangeHomaValueResponse;

/** An admin changes one Homa-set value, with a reason. */
public interface AdminHomaValueChanger {

    ChangeHomaValueResponse change(ChangeHomaValueRequest request);
}
