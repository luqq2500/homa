package account.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.AccountDetailRequiredException;
import account.domain.exception.InvalidCoordinatesException;
import account.domain.exception.InvalidPhoneNumberException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Phone numbers, addresses and photos on accounts. */
class AccountDetailsTest {

    @Nested
    class PhoneNumbers {

        @ParameterizedTest(name = "{0} is a Malaysian mobile number")
        @ValueSource(strings = {"+60123456789", "+601123456789", "+60198765432"})
        void malaysianMobileNumbersAreAccepted(String phone) {
            assertThat(PhoneNumber.of(phone).value()).isEqualTo(phone);
            assertThat(PhoneNumber.of(phone)).hasToString(phone);
        }

        @ParameterizedTest(name = "{0} is not a Malaysian mobile number")
        @NullSource
        @ValueSource(strings = {"0123456789", "+6012345678", "+6011234567890", "+60312345678", "+65123456789", "+60 12345 6789"})
        void otherNumbersAreRejected(String phone) {
            assertThatThrownBy(() -> PhoneNumber.of(phone)).isInstanceOf(InvalidPhoneNumberException.class);
        }
    }

    @Nested
    class Addresses {

        @Test
        void anAddressKeepsTheStreetCityAndCoordinates() {
            Address address = Address.of(" 12 Jalan Teknokrat 3 ", City.of(" Cyberjaya "), Coordinates.of(2.9213, 101.6559));

            assertThat(address.street()).isEqualTo("12 Jalan Teknokrat 3");
            assertThat(address.city()).isEqualTo(City.of("Cyberjaya"));
            assertThat(address.coordinates().latitude()).isEqualTo(2.9213);
            assertThat(address.coordinates().longitude()).isEqualTo(101.6559);
        }

        @Test
        void anAddressNeedsAStreetACityAndCoordinates() {
            assertThatThrownBy(() -> Address.of(" ", City.of("Cyberjaya"), Coordinates.of(2.9, 101.6)))
                    .isInstanceOf(AccountDetailRequiredException.class)
                    .hasMessage("A street address is required");
            assertThatThrownBy(() -> Address.of("12 Jalan", null, Coordinates.of(2.9, 101.6)))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Address.of("12 Jalan", City.of("Cyberjaya"), null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> City.of("")).isInstanceOf(AccountDetailRequiredException.class);
        }

        @ParameterizedTest(name = "{0}, {1} is a place on Earth")
        @CsvSource({"90, 180", "-90, -180", "0, 0"})
        void coordinatesAtTheEdgesOfTheMapAreAccepted(double latitude, double longitude) {
            assertThat(Coordinates.of(latitude, longitude).latitude()).isEqualTo(latitude);
        }

        @ParameterizedTest(name = "{0}, {1} is not a place on Earth")
        @CsvSource({"90.0001, 0", "-90.0001, 0", "0, 180.0001", "0, -180.0001"})
        void coordinatesOffTheMapAreRejected(double latitude, double longitude) {
            assertThatThrownBy(() -> Coordinates.of(latitude, longitude)).isInstanceOf(InvalidCoordinatesException.class);
        }
    }

    @Test
    void aPhotoNeedsAReference() {
        assertThat(Photo.of(" photos/aina.jpg ").reference()).isEqualTo("photos/aina.jpg");
        assertThatThrownBy(() -> Photo.of(null)).isInstanceOf(AccountDetailRequiredException.class);
    }
}
