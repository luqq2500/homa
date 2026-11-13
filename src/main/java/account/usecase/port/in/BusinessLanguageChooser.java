package account.usecase.port.in;

import account.usecase.dto.ChooseBusinessLanguageRequest;
import account.usecase.dto.ChooseBusinessLanguageResponse;

/** A business chooses the language of its provider app. */
public interface BusinessLanguageChooser {

    ChooseBusinessLanguageResponse choose(ChooseBusinessLanguageRequest request);
}
