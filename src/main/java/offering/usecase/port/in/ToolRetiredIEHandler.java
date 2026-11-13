package offering.usecase.port.in;

import ie.events.ToolRetiredIE;

/** A retired tool comes off every provider's listing. */
public interface ToolRetiredIEHandler {

    void handle(ToolRetiredIE event);
}
