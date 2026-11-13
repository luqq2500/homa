package reservation.usecase.port.in;

import ie.events.CaseDecidedIE;

/** Homa's decision ends the dispute. */
public interface CaseDecidedIEHandler {

    void handle(CaseDecidedIE event);
}
