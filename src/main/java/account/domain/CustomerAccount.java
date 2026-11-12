package account.domain;

import java.util.Objects;

/**
 * A customer: registered with a confirmed phone number, with a name providers see and the address Homa
 * uses to match providers by city and to locate check-in.
 */
public final class CustomerAccount {

    private final String id;
    private final PhoneNumber phone;
    private String name;
    private Address address;

    private CustomerAccount(String id, PhoneNumber phone, String name, Address address) {
        this.id = id;
        this.phone = phone;
        this.name = name;
        this.address = address;
    }

    public static CustomerAccount register(PhoneVerification verified, String name, Address address) {
        verified.requireConfirmed();
        return new CustomerAccount(
                new CustomerId().generate(),
                verified.phone(),
                AccountText.require(name, "A customer's name"),
                Objects.requireNonNull(address));
    }

    /**
     * The customer changes their name, for example to correct it. Returns whether it changed: the same
     * name changes nothing.
     */
    public boolean rename(String newName) {
        String renamed = AccountText.require(newName, "A customer's name");
        boolean changed = !renamed.equals(name);
        name = renamed;
        return changed;
    }

    public void changeAddress(Address newAddress) {
        address = Objects.requireNonNull(newAddress);
    }

    public String id() {
        return id;
    }

    public PhoneNumber phone() {
        return phone;
    }

    public String name() {
        return name;
    }

    public Address address() {
        return address;
    }
}
