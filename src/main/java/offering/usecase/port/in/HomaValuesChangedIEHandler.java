package offering.usecase.port.in;

import ie.events.HomaValuesChangedIE;

/** When Homa narrows the duration range, each provider's hours shrink to fit inside it. */
public interface HomaValuesChangedIEHandler {

    void handle(HomaValuesChangedIE event);
}
