package account.usecase.port.in;

import account.usecase.dto.ChooseCustomerLanguageRequest;
import account.usecase.dto.ChooseCustomerLanguageResponse;

/** A customer chooses the language of their app. */
public interface CustomerLanguageChooser {

    ChooseCustomerLanguageResponse choose(ChooseCustomerLanguageRequest request);
}
