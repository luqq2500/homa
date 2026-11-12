package catalogue.usecase.port.in;

import catalogue.usecase.dto.PublishServiceAreaRequest;
import catalogue.usecase.dto.PublishServiceAreaResponse;

/** Homa's founders publish the cities Homa serves. */
public interface HomaServiceAreaPublisher {

    PublishServiceAreaResponse publish(PublishServiceAreaRequest request);
}
