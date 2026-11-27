package com.homa.reservation.usecase;

import com.homa.ie.port.ReservationIE;
import com.homa.reservation.domain.Checkout;
import com.homa.reservation.domain.CheckoutId;
import com.homa.reservation.usecase.port.out.CheckoutRepository;
import com.homa.reservation.usecase.port.out.CompletionPhotoStore;
import com.homa.reservation.usecase.port.out.CompletionReminderLog;
import com.homa.reservation.usecase.port.out.CompletionReminderNotifier;
import com.homa.reservation.usecase.port.out.EventIds;
import com.homa.reservation.usecase.port.out.ProcessedMessageStore;
import com.homa.reservation.usecase.port.out.ReservationIEPublisher;
import com.homa.reservation.usecase.port.out.TimeSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** The remaining port/out fakes: in memory, recording what they were given, and holding no rules. */
public final class InMemoryPorts {

    private InMemoryPorts() {
    }

    public static final class Checkouts implements CheckoutRepository {
        private final Map<CheckoutId, Checkout> stored = new LinkedHashMap<>();

        @Override
        public Optional<Checkout> findById(CheckoutId id) {
            return Optional.ofNullable(stored.get(id));
        }

        @Override
        public void save(Checkout checkout) {
            stored.put(checkout.id(), checkout);
        }

        public List<Checkout> all() {
            return List.copyOf(stored.values());
        }
    }

    public static final class RecordingPublisher implements ReservationIEPublisher {
        private final List<ReservationIE> published = new ArrayList<>();

        @Override
        public void publish(ReservationIE event) {
            published.add(event);
        }

        public List<ReservationIE> published() {
            return List.copyOf(published);
        }

        /** The one event published since the last clear, of this type. */
        public <E extends ReservationIE> E single(Class<E> type) {
            if (published.size() != 1) {
                throw new AssertionError("Expected one event, found " + published);
            }
            return type.cast(published.get(0));
        }

        public void clear() {
            published.clear();
        }
    }

    public static final class ProcessedMessages implements ProcessedMessageStore {
        private final Set<String> handled = new HashSet<>();

        @Override
        public boolean recordFirstHandling(String eventId, String handler) {
            return handled.add(handler + "|" + eventId);
        }

        public boolean handled(String eventId, String handler) {
            return handled.contains(handler + "|" + eventId);
        }
    }

    public static final class SettableTime implements TimeSource {
        private Instant now;

        public SettableTime(Instant now) {
            this.now = now;
        }

        @Override
        public Instant now() {
            return now;
        }

        public void set(Instant moment) {
            this.now = moment;
        }
    }

    public static final class SequentialEventIds implements EventIds {
        private int next = 1;

        @Override
        public String next() {
            return "reservation-event-" + next++;
        }
    }

    public static final class PhotoStore implements CompletionPhotoStore {
        private final Set<String> uploaded = new HashSet<>();

        @Override
        public boolean holds(String photoRef) {
            return uploaded.contains(photoRef);
        }

        public void upload(String photoRef) {
            uploaded.add(photoRef);
        }
    }

    public static final class ReminderLog implements CompletionReminderLog {
        private final Set<String> sent = new HashSet<>();

        @Override
        public boolean recordFirstSending(String reservationId, Instant reminderAt) {
            return sent.add(reservationId + "@" + reminderAt);
        }
    }

    public record Reminder(String reservationId, String providerId, Instant jobEnd, Instant reminderAt) {
    }

    public static final class RecordingNotifier implements CompletionReminderNotifier {
        private final List<Reminder> sent = new ArrayList<>();

        @Override
        public void remindToMarkCompleted(String reservationId, String providerId, Instant jobEnd,
                Instant reminderAt) {
            sent.add(new Reminder(reservationId, providerId, jobEnd, reminderAt));
        }

        public List<Reminder> sent() {
            return List.copyOf(sent);
        }
    }
}
