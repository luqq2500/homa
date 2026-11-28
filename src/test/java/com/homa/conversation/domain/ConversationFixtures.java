package com.homa.conversation.domain;

import static com.homa.fixtures.HomaFixtures.at;

import com.homa.fixtures.HomaFixtures;
import java.time.Instant;
import java.util.Optional;

/**
 * The §0 example as Conversation sees it: Aisyah, Kemas Home and her standard Saturday reservation, requested on
 * Monday 30 November at 20:00. Check-in Period from Saturday 9:45; Protection Window until Sunday 14:00.
 */
public final class ConversationFixtures {

    private ConversationFixtures() {
    }

    public static final CustomerId AISYAH = new CustomerId(HomaFixtures.AISYAH_CUSTOMER_ID);
    public static final CustomerId DANIEL = new CustomerId("customer-daniel");
    public static final ProviderId KEMAS_HOME = new ProviderId(HomaFixtures.KEMAS_HOME_PROVIDER_ID);
    public static final ReservationId SATURDAYS_RESERVATION = new ReservationId("reservation-saturday");

    public static final Instant REQUESTED_AT = HomaFixtures.REQUESTED_AT;
    public static final Instant CHECK_IN_START = HomaFixtures.CHECK_IN_PERIOD.start();
    public static final Instant PROTECTION_WINDOW_END = HomaFixtures.PROTECTION_WINDOW.end();

    public static final Instant WEDNESDAY_9_00 = at(2026, 12, 2, 9, 0);
    public static final Instant SATURDAY_9_40 = at(2026, 12, 5, 9, 40);
    public static final Instant SATURDAY_9_50 = at(2026, 12, 5, 9, 50);
    public static final Instant SUNDAY_MORNING = at(2026, 12, 6, 9, 0);
    public static final Instant MONDAY_9_00 = at(2026, 12, 7, 9, 0);

    /** Saturday's reservation as Requested: its chat is open until Sunday 14:00. */
    public static ReservationChatWindow requestedWindow() {
        return ReservationChatWindow.open(SATURDAYS_RESERVATION, AISYAH, KEMAS_HOME, PROTECTION_WINDOW_END);
    }

    /** Aisyah, registered and not suspended. */
    public static CustomerStanding aisyah() {
        return CustomerStanding.registered(AISYAH);
    }

    /** Aisyah's question about a ladder, asked on Monday 20:00. */
    public static Conversation aisyahsEnquiry() {
        return Conversation.startEnquiry(aisyah(), KEMAS_HOME, "Do you bring a ladder?", REQUESTED_AT);
    }

    /** The chat of Saturday's reservation, opened when it was Requested. */
    public static Conversation saturdaysChat() {
        return Conversation.openReservationChat(requestedWindow(), REQUESTED_AT);
    }

    public static Optional<ReservationChatWindow> windowOf(ReservationChatWindow window) {
        return Optional.of(window);
    }
}
