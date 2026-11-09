package account.domain;

/**
 * A business provider's SSM registration, as the eKYB result read it: the SSM number and the registered
 * business name. Homa keeps it for its records; the provider page shows the profile's display name.
 */
public record BusinessRegistration(String ssmNumber, String registeredName) {

    public BusinessRegistration {
        ssmNumber = AccountText.require(ssmNumber, "An SSM registration number");
        registeredName = AccountText.require(registeredName, "A registered business name");
    }
}
