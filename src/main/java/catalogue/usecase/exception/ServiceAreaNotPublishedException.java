package catalogue.usecase.exception;

/** The cities Homa serves have not been published yet. */
public class ServiceAreaNotPublishedException extends CatalogueUsecaseException {

    public ServiceAreaNotPublishedException() {
        super("The service area has not been published yet");
    }
}
