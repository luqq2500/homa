package catalogue.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Feature: Service area. */
class ServiceAreaCoverageTest {

    @ParameterizedTest(name = "the service area covers {0} is {1}")
    @CsvSource({
        "Salak Tinggi, true",
        "Cyberjaya,    true",
        "Putrajaya,    true",
        "Kajang,       false"
    })
    void onlyLaunchCitiesAreCovered(String city, boolean covered) {
        assertThat(ServiceArea.launch().covers(City.of(city))).isEqualTo(covered);
    }

    @Test
    void theLaunchAreaIsExactlyTheThreeLaunchCities() {
        ServiceArea area = ServiceArea.launch();

        assertThat(area.cities())
                .containsExactlyInAnyOrder(City.of("Salak Tinggi"), City.of("Cyberjaya"), City.of("Putrajaya"));
        assertThatThrownBy(() -> area.cities().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(area.id()).isNotBlank().isNotEqualTo(ServiceArea.launch().id());
    }
}
