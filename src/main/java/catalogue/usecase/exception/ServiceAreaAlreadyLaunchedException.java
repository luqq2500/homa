package catalogue.usecase.exception;

/** Homa's service area is launched once. */
public class ServiceAreaAlreadyLaunchedException extends CatalogueUsecaseException {

    public ServiceAreaAlreadyLaunchedException() {
        super("Homa's service area is already launched");
    }
}
