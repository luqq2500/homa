package account.usecase;

import account.domain.Address;
import account.domain.CustomerAccount;
import account.usecase.dto.ChangeAddressRequest;
import account.usecase.dto.ChangeAddressResponse;
import account.usecase.port.in.CustomerAddressChanger;
import account.usecase.port.out.CustomerAccountRepository;
import account.usecase.port.out.Geocoder;

/**
 * Changes a customer's address, found by the geoservice. Reservations already made keep the address they
 * were made with.
 */
public final class CustomerChangeAddress implements CustomerAddressChanger {

    private final CustomerAccountRepository customers;
    private final Geocoder geocoder;

    public CustomerChangeAddress(CustomerAccountRepository customers, Geocoder geocoder) {
        this.customers = customers;
        this.geocoder = geocoder;
    }

    @Override
    public ChangeAddressResponse change(ChangeAddressRequest request) {
        CustomerAccount customer = Accounts.customer(customers, request.customerId());
        Address address = Addresses.locate(geocoder, request.street());

        customer.changeAddress(address);

        customers.save(customer);
        return new ChangeAddressResponse(address.street(), address.city().name());
    }
}
