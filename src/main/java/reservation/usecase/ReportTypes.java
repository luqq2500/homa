package reservation.usecase;

import reservation.domain.ReportType;
import reservation.usecase.exception.UnknownChoiceException;

/** Reads the type of report a person chose. */
final class ReportTypes {

    private ReportTypes() {
    }

    static ReportType called(String name) {
        try {
            return ReportType.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException unknown) {
            throw new UnknownChoiceException(name);
        }
    }
}
