package com.homa.conversation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConversationIdentitiesTest {

    static Stream<Arguments> identities() {
        return Stream.of(
                Arguments.of("conversation id", (Function<String, Object>) ConversationId::new),
                Arguments.of("message id", (Function<String, Object>) MessageId::new),
                Arguments.of("customer id", (Function<String, Object>) CustomerId::new),
                Arguments.of("provider id", (Function<String, Object>) ProviderId::new),
                Arguments.of("reservation id", (Function<String, Object>) ReservationId::new));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("identities")
    @DisplayName("An id is never blank or missing.")
    void anIdIsNeverBlankOrMissing(String name, Function<String, Object> id) {
        assertThatThrownBy(() -> id.apply(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(name);
        assertThatThrownBy(() -> id.apply("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> id.apply("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThat(id.apply("an-id")).isNotNull();
    }

    @Test
    @DisplayName("Conversations and messages get a new id each time.")
    void generatedIdsAreUnique() {
        assertThat(ConversationId.generate()).isNotEqualTo(ConversationId.generate());
        assertThat(MessageId.generate()).isNotEqualTo(MessageId.generate());
        assertThat(ConversationId.generate().value()).isNotBlank();
        assertThat(MessageId.generate().value()).isNotBlank();
    }
}
