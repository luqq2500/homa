package account.usecase.port.in;

import account.usecase.dto.RequestDataRequest;
import account.usecase.dto.RequestDataResponse;

/** A customer or business asks to see, correct, copy or delete their personal data. */
public interface UserDataRequester {

    RequestDataResponse request(RequestDataRequest request);
}
