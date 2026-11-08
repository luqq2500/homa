package catalogue.usecase.port.in;

import catalogue.usecase.dto.CreateShelfRequest;
import catalogue.usecase.dto.CreateShelfResponse;

/** Homa creates a homepage shelf. */
public interface HomaShelfCreator {

    CreateShelfResponse create(CreateShelfRequest request);
}
