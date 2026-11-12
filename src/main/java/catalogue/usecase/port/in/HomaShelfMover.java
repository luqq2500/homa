package catalogue.usecase.port.in;

import catalogue.usecase.dto.MoveShelfRequest;
import catalogue.usecase.dto.MoveShelfResponse;

/** Homa's founders move a homepage shelf. */
public interface HomaShelfMover {

    MoveShelfResponse move(MoveShelfRequest request);
}
