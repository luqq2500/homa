package com.homa.provider.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** The identities of providers, the people who own them and their team members. */
class ProviderIdentityTest {

    @Test
    @Tag("PRV-1")
    @DisplayName("Every new provider gets its own id.")
    void eachNewProviderGetsItsOwnId() {
        ProviderId first = ProviderId.generate();

        assertThat(first.value()).isNotBlank();
        assertThat(first).isNotEqualTo(ProviderId.generate());
        assertThat(new ProviderId(first.value())).isEqualTo(first);
    }

    @Test
    @Tag("HLP-2")
    @DisplayName("Every new team member gets their own id.")
    void eachNewTeamMemberGetsTheirOwnId() {
        TeamMemberId first = TeamMemberId.generate();

        assertThat(first.value()).isNotBlank();
        assertThat(first).isNotEqualTo(TeamMemberId.generate());
        assertThat(new TeamMemberId(first.value())).isEqualTo(first);
    }

    @Test
    @Tag("IDN-3")
    @DisplayName("A person is recognised by the personId Verification gave them, never by their identity document number.")
    void personIsRecognisedByTheirPersonId() {
        assertThat(new PersonId("person-azman")).isEqualTo(new PersonId("person-azman"));
        assertThat(new PersonId("person-azman")).isNotEqualTo(new PersonId("person-hana"));
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("A provider id, person id or team member id can't be missing or blank.")
    void idsCannotBeMissingOrBlank() {
        assertThatThrownBy(() -> new ProviderId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProviderId(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PersonId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PersonId(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TeamMemberId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TeamMemberId("")).isInstanceOf(IllegalArgumentException.class);
    }
}
