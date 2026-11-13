package kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GeoPointTest {

    @Test
    void aPointKeepsItsLatitudeAndLongitude() {
        GeoPoint cyberjaya = new GeoPoint(2.9213, 101.6559);
        assertThat(cyberjaya.latitude()).isEqualTo(2.9213);
        assertThat(cyberjaya.longitude()).isEqualTo(101.6559);
    }

    @Test
    void thePolesAndTheDateLineAreOnTheMap() {
        assertThat(new GeoPoint(90, 180)).isEqualTo(new GeoPoint(90, 180));
        assertThat(new GeoPoint(-90, -180)).isEqualTo(new GeoPoint(-90, -180));
    }

    @Test
    void aLatitudeBeyondThePolesIsNotOnTheMap() {
        assertThatThrownBy(() -> new GeoPoint(90.0001, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeoPoint(-90.0001, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeoPoint(Double.NaN, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aLongitudeBeyondTheDateLineIsNotOnTheMap() {
        assertThatThrownBy(() -> new GeoPoint(0, 180.0001)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeoPoint(0, -180.0001)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeoPoint(0, Double.NaN)).isInstanceOf(IllegalArgumentException.class);
    }
}
