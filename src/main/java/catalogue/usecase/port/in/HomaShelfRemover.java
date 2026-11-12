package catalogue.usecase.port.in;

import catalogue.usecase.dto.RemoveShelfRequest;
import catalogue.usecase.dto.RemoveShelfResponse;

/** Homa's founders remove a homepage shelf. */
public interface HomaShelfRemover {

    RemoveShelfResponse remove(RemoveShelfRequest request);
}
