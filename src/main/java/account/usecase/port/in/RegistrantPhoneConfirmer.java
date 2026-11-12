package account.usecase.port.in;

import account.usecase.dto.ConfirmPhoneRequest;
import account.usecase.dto.ConfirmPhoneResponse;

/** Someone registering enters the SMS code. */
public interface RegistrantPhoneConfirmer {

    ConfirmPhoneResponse confirm(ConfirmPhoneRequest request);
}
