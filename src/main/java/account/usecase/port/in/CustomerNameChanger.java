package account.usecase.port.in;

import account.usecase.dto.ChangeNameRequest;
import account.usecase.dto.ChangeNameResponse;

/** A customer changes their name. */
public interface CustomerNameChanger {

    ChangeNameResponse change(ChangeNameRequest request);
}
