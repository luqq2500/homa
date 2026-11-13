package account.usecase.port.in;

import account.usecase.dto.CloseBusinessAccountRequest;
import account.usecase.dto.CloseBusinessAccountResponse;

/** A business closes its account. */
public interface BusinessAccountCloser {

    CloseBusinessAccountResponse close(CloseBusinessAccountRequest request);
}
