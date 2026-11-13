package account.usecase.port.in;

import account.usecase.dto.RequestPhoneCodeRequest;
import account.usecase.dto.RequestPhoneCodeResponse;

/** Someone asks for an SMS code to verify their phone number. */
public interface GuestPhoneCodeRequester {

    RequestPhoneCodeResponse request(RequestPhoneCodeRequest request);
}
