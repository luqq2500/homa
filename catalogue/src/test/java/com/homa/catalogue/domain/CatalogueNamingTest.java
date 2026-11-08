package com.homa.catalogue.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.catalogue.domain.exception.CatalogueNameRequiredException;
import com.homa.catalogue.domain.exception.InvalidCatalogueCodeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Codes and names on Homa's lists. */
class CatalogueNamingTest {

    @ParameterizedTest(name = "{0} is a valid code")
    @ValueSource(strings = {"KITCHEN", "KITCHEN_GREASE", "LAUNDRY_FOLD", "TOILET_CARE_2", "A"})
    void codesAreUpperCaseWordsJoinedByUnderscores(String code) {
        assertThat(JobCode.of(code).value()).isEqualTo(code);
        assertThat(ToolCode.of(code).value()).isEqualTo(code);
        assertThat(JobCode.of(code)).hasToString(code);
        assertThat(ToolCode.of(code)).hasToString(code);
    }

    @ParameterizedTest(name = "\"{0}\" is not a valid code")
    @NullSource
    @ValueSource(strings = {"", "kitchen", "Kitchen_Grease", "KITCHEN GREASE", "_KITCHEN", "KITCHEN_", "KITCHEN__GREASE", "2KITCHEN", "KITCHEN-GREASE"})
    void anythingElseIsNotAValidCode(String code) {
        assertThatThrownBy(() -> JobCode.of(code)).isInstanceOf(InvalidCatalogueCodeException.class);
        assertThatThrownBy(() -> ToolCode.of(code)).isInstanceOf(InvalidCatalogueCodeException.class);
    }

    @Test
    void anInvalidCodeIsNamedInTheMessage() {
        assertThatThrownBy(() -> JobCode.of("kitchen")).hasMessage("Not a valid catalogue code: \"kitchen\"");
    }

    @Test
    void groupsAndCitiesAreNamedWithoutSurroundingSpaces() {
        assertThat(JobGroup.of(" Living room ").name()).isEqualTo("Living room");
        assertThat(City.of(" Cyberjaya ")).isEqualTo(City.of("Cyberjaya"));
    }

    @ParameterizedTest(name = "\"{0}\" is not a name")
    @NullSource
    @ValueSource(strings = {"", "   "})
    void groupsAndCitiesNeedAName(String name) {
        assertThatThrownBy(() -> JobGroup.of(name))
                .isInstanceOf(CatalogueNameRequiredException.class)
                .hasMessage("A job group needs a name");
        assertThatThrownBy(() -> City.of(name))
                .isInstanceOf(CatalogueNameRequiredException.class)
                .hasMessage("A city needs a name");
    }
}
