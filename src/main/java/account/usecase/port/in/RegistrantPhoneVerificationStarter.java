package account.usecase.port.in;

import account.usecase.dto.StartPhoneVerificationRequest;
import account.usecase.dto.StartPhoneVerificationResponse;

/** Someone registering, as a customer or a business, asks for an SMS code. */
public interface RegistrantPhoneVerificationStarter {

    StartPhoneVerificationResponse start(StartPhoneVerificationRequest request);
}
