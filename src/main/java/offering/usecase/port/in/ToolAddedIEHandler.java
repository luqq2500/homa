package offering.usecase.port.in;

import ie.events.ToolAddedIE;

/** A tool Homa added can now be picked. */
public interface ToolAddedIEHandler {

    void handle(ToolAddedIE event);
}
