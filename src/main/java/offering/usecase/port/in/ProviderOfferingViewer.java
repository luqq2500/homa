package offering.usecase.port.in;

import offering.usecase.dto.ViewOfferingRequest;
import offering.usecase.dto.ViewOfferingResponse;

/** A provider sees their offering and what is still missing to go live. */
public interface ProviderOfferingViewer {

    ViewOfferingResponse view(ViewOfferingRequest request);
}
