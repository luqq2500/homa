package catalogue.usecase.exception;

/** Homa has not launched its service area yet. */
public class ServiceAreaNotLaunchedException extends CatalogueUsecaseException {

    public ServiceAreaNotLaunchedException() {
        super("Homa's service area has not been launched");
    }
}
