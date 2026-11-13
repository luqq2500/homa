package offering.usecase.port.in;

import offering.usecase.dto.ChooseCancellationTierRequest;
import offering.usecase.dto.ChooseCancellationTierResponse;

/** A provider chooses their cancellation policy from Homa's tiers. */
public interface ProviderCancellationTierChooser {

    ChooseCancellationTierResponse choose(ChooseCancellationTierRequest request);
}
