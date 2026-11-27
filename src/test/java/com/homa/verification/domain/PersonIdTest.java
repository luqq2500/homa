package com.homa.verification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PersonIdTest {

    @Test
    @Tag("IDN-3")
    @DisplayName("Other contexts recognise Siti by her personId, so every verified person gets one of their own.")
    void everyPersonGetsTheirOwnId() {
        PersonId first = PersonId.generate();
        PersonId second = PersonId.generate();

        assertThat(first).isNotEqualTo(second);
        assertThat(first.value()).isNotBlank();
    }

    @Test
    @Tag("IDN-3")
    @DisplayName("A personId can't be blank, or it would recognise no one.")
    void aPersonIdCannotBeBlank() {
        assertThatThrownBy(() -> new PersonId(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A person id must not be blank");
        assertThatThrownBy(() -> new PersonId(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
