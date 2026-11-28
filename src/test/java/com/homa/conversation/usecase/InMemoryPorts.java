package com.homa.conversation.usecase;

import com.homa.conversation.domain.Conversation;
import com.homa.conversation.domain.ConversationId;
import com.homa.conversation.domain.CustomerId;
import com.homa.conversation.domain.CustomerStanding;
import com.homa.conversation.domain.ProviderId;
import com.homa.conversation.domain.ReservationChatWindow;
import com.homa.conversation.domain.ReservationId;
import com.homa.conversation.usecase.port.out.ConversationRepository;
import com.homa.conversation.usecase.port.out.CustomerStandingRepository;
import com.homa.conversation.usecase.port.out.ProcessedMessageStore;
import com.homa.conversation.usecase.port.out.ProviderOwners;
import com.homa.conversation.usecase.port.out.ReservationChatWindowRepository;
import com.homa.conversation.usecase.port.out.TimeSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** The port/out fakes: in memory, recording what they were given, and holding no rules. */
public final class InMemoryPorts {

    private InMemoryPorts() {
    }

    /** Conversations in a map. It keeps the instances it was given and counts each save. */
    public static final class Conversations implements ConversationRepository {
        private final Map<ConversationId, Conversation> stored = new LinkedHashMap<>();
        private final List<ConversationId> saves = new ArrayList<>();

        @Override
        public Optional<Conversation> findById(ConversationId id) {
            return Optional.ofNullable(stored.get(id));
        }

        @Override
        public Optional<Conversation> findByReservationId(ReservationId reservationId) {
            return stored.values().stream()
                    .filter(conversation -> conversation.reservationId().filter(reservationId::equals).isPresent())
                    .findFirst();
        }

        @Override
        public void save(Conversation conversation) {
            stored.put(conversation.id(), conversation);
            saves.add(conversation.id());
        }

        /** Puts a conversation in place without counting it as a save by the code under test. */
        public Conversation given(Conversation conversation) {
            stored.put(conversation.id(), conversation);
            return conversation;
        }

        public List<Conversation> all() {
            return List.copyOf(stored.values());
        }

        /** The ids of the conversations saved, in order, one entry per save. */
        public List<ConversationId> saves() {
            return List.copyOf(saves);
        }
    }

    public static final class ChatWindows implements ReservationChatWindowRepository {
        private final Map<ReservationId, ReservationChatWindow> stored = new LinkedHashMap<>();
        private int saves;

        @Override
        public Optional<ReservationChatWindow> findByReservationId(ReservationId reservationId) {
            return Optional.ofNullable(stored.get(reservationId));
        }

        @Override
        public void save(ReservationChatWindow window) {
            stored.put(window.reservationId(), window);
            saves++;
        }

        public ReservationChatWindow given(ReservationChatWindow window) {
            stored.put(window.reservationId(), window);
            return window;
        }

        public int count() {
            return stored.size();
        }

        public int saves() {
            return saves;
        }
    }

    public static final class CustomerStandings implements CustomerStandingRepository {
        private final Map<CustomerId, CustomerStanding> stored = new LinkedHashMap<>();
        private int saves;

        @Override
        public Optional<CustomerStanding> findById(CustomerId customerId) {
            return Optional.ofNullable(stored.get(customerId));
        }

        @Override
        public void save(CustomerStanding standing) {
            stored.put(standing.customerId(), standing);
            saves++;
        }

        public CustomerStanding given(CustomerStanding standing) {
            stored.put(standing.customerId(), standing);
            return standing;
        }

        public int count() {
            return stored.size();
        }

        public int saves() {
            return saves;
        }
    }

    public static final class Owners implements ProviderOwners {
        private final Map<ProviderId, String> owners = new LinkedHashMap<>();

        @Override
        public Optional<String> ownerOf(ProviderId providerId) {
            return Optional.ofNullable(owners.get(providerId));
        }

        @Override
        public void record(ProviderId providerId, String ownerPersonId) {
            owners.put(providerId, ownerPersonId);
        }

        public int count() {
            return owners.size();
        }
    }

    /** The (eventId, handler) pairs handled so far, like the processed_message table's primary key. */
    public static final class ProcessedMessages implements ProcessedMessageStore {
        private final Set<String> handled = new HashSet<>();

        @Override
        public boolean recordFirstHandling(String eventId, String handler) {
            return handled.add(handler + "|" + eventId);
        }

        public boolean hasHandled(String eventId, String handler) {
            return handled.contains(handler + "|" + eventId);
        }
    }

    /** A clock that stays where a test sets it. */
    public static final class FixedTime implements TimeSource {
        private Instant now;

        public FixedTime(Instant now) {
            this.now = now;
        }

        @Override
        public Instant now() {
            return now;
        }

        public void setNow(Instant now) {
            this.now = now;
        }
    }
}
