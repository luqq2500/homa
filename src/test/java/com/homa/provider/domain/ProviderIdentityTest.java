package com.homa.provider.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The identities of providers and their team members. */
class ProviderIdentityTest {

    @Test
    @DisplayName("Every new provider gets its own id.")
    void eachNewProviderGetsItsOwnId() {
        ProviderId first = ProviderId.generate();

        assertThat(first.value()).isNotBlank();
        assertThat(first).isNotEqualTo(ProviderId.generate());
        assertThat(new ProviderId(first.value())).isEqualTo(first);
    }

    @Test
    @DisplayName("Every new team member gets their own id.")
    void eachNewTeamMemberGetsTheirOwnId() {
        TeamMemberId first = TeamMemberId.generate();

        assertThat(first.value()).isNotBlank();
        assertThat(first).isNotEqualTo(TeamMemberId.generate());
        assertThat(new TeamMemberId(first.value())).isEqualTo(first);
    }

    @Test
    @DisplayName("A provider id or team member id can't be missing or blank.")
    void idsCannotBeMissingOrBlank() {
        assertThatThrownBy(() -> new ProviderId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProviderId(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TeamMemberId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TeamMemberId("")).isInstanceOf(IllegalArgumentException.class);
    }
}
