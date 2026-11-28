package com.homa.conversation.usecase;

import static com.homa.conversation.domain.ConversationFixtures.AISYAH;
import static com.homa.conversation.domain.ConversationFixtures.KEMAS_HOME;
import static com.homa.conversation.domain.ConversationFixtures.REQUESTED_AT;

import com.homa.conversation.domain.Conversation;
import com.homa.conversation.domain.CustomerStanding;
import com.homa.conversation.domain.ReservationId;
import com.homa.conversation.usecase.ie.HandleCustomerRegisteredIE;
import com.homa.conversation.usecase.ie.HandleCustomerVerifiedIE;
import com.homa.conversation.usecase.ie.HandleProviderOpenedIE;
import com.homa.conversation.usecase.ie.HandleReservationCancelledIE;
import com.homa.conversation.usecase.ie.HandleReservationDeclinedIE;
import com.homa.conversation.usecase.ie.HandleReservationExpiredIE;
import com.homa.conversation.usecase.ie.HandleReservationRequestedIE;
import com.homa.conversation.usecase.ie.HandleReservationWithdrawnIE;
import com.homa.conversation.usecase.InMemoryPorts.ChatWindows;
import com.homa.conversation.usecase.InMemoryPorts.Conversations;
import com.homa.conversation.usecase.InMemoryPorts.CustomerStandings;
import com.homa.conversation.usecase.InMemoryPorts.FixedTime;
import com.homa.conversation.usecase.InMemoryPorts.Owners;
import com.homa.conversation.usecase.InMemoryPorts.ProcessedMessages;
import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.CustomerRegisteredIE;
import com.homa.ie.events.CustomerVerifiedIE;
import com.homa.ie.events.ProviderOpenedIE;
import com.homa.ie.events.ReservationCancelledIE;
import com.homa.ie.events.ReservationDeclinedIE;
import com.homa.ie.events.ReservationExpiredIE;
import com.homa.ie.events.ReservationRequestedIE;
import com.homa.ie.events.ReservationWithdrawnIE;
import com.homa.kernel.Money;
import java.time.Instant;

/**
 * Conversation's use cases and handlers wired to in-memory ports, with Aisyah registered and Kemas Home open under
 * its owner Azman, and the clock at Monday 20:00 when Aisyah's Saturday reservation is Requested. Also builds the
 * events they consume, from the §0 example.
 */
public final class ConversationTestBed {

    public static final String AISYAH_ID = AISYAH.value();
    public static final String KEMAS_HOME_ID = KEMAS_HOME.value();
    public static final String AZMAN_PERSON_ID = "person-azman";
    public static final String STRANGER_ID = "person-stranger";
    public static final String SATURDAYS_RESERVATION_ID = "reservation-saturday";

    public final ProcessedMessages processedMessages = new ProcessedMessages();
    public final Conversations conversations = new Conversations();
    public final ChatWindows windows = new ChatWindows();
    public final CustomerStandings customers = new CustomerStandings();
    public final Owners owners = new Owners();
    public final FixedTime time = new FixedTime(REQUESTED_AT);

    public final CustomerStartEnquiry startEnquiry = new CustomerStartEnquiry(customers, owners, conversations, time);
    public final PartySendMessage sendMessage = new PartySendMessage(conversations, windows, owners, time);
    public final ViewConversation viewConversation = new ViewConversation(conversations, windows, owners, time);

    public final HandleReservationRequestedIE reservationRequested =
            new HandleReservationRequestedIE(processedMessages, windows, conversations);
    public final HandleReservationWithdrawnIE reservationWithdrawn =
            new HandleReservationWithdrawnIE(processedMessages, windows);
    public final HandleReservationDeclinedIE reservationDeclined =
            new HandleReservationDeclinedIE(processedMessages, windows);
    public final HandleReservationExpiredIE reservationExpired =
            new HandleReservationExpiredIE(processedMessages, windows);
    public final HandleReservationCancelledIE reservationCancelled =
            new HandleReservationCancelledIE(processedMessages, windows);
    public final HandleCustomerRegisteredIE customerRegistered =
            new HandleCustomerRegisteredIE(processedMessages, customers);
    public final HandleCustomerVerifiedIE customerVerified =
            new HandleCustomerVerifiedIE(processedMessages, customers);
    public final HandleProviderOpenedIE providerOpened = new HandleProviderOpenedIE(processedMessages, owners);

    private ConversationTestBed() {
    }

    /** Aisyah is a registered customer and Kemas Home is a provider whose owner is Azman. */
    public static ConversationTestBed withKemasHomeAndAisyah() {
        ConversationTestBed bed = new ConversationTestBed();
        bed.customers.given(CustomerStanding.registered(AISYAH));
        bed.owners.record(KEMAS_HOME, AZMAN_PERSON_ID);
        return bed;
    }

    /** Aisyah's Saturday reservation is Requested at Monday 20:00, which opens its chat. Returns the chat's id. */
    public String requestSaturdaysReservation() {
        reservationRequested.handle(reservationRequestedEvent("requested-1"));
        return saturdaysChat().id().value();
    }

    /** The chat of Aisyah's Saturday reservation, once it is Requested. */
    public Conversation saturdaysChat() {
        return conversations.findByReservationId(new ReservationId(SATURDAYS_RESERVATION_ID)).orElseThrow();
    }

    /** Aisyah's Saturday reservation, Requested on Monday at 20:00 (§0). */
    public static ReservationRequestedIE reservationRequestedEvent(String eventId) {
        return new ReservationRequestedIE(eventId, SATURDAYS_RESERVATION_ID, "checkout-1", AISYAH_ID, KEMAS_HOME_ID,
                HomaFixtures.CHECK_IN_PERIOD.start(), HomaFixtures.JOB_START, HomaFixtures.JOB_END,
                HomaFixtures.TOTAL_RESERVATION_FEE, HomaFixtures.ACCEPTANCE_DEADLINE,
                HomaFixtures.PROTECTION_WINDOW.end(), REQUESTED_AT, REQUESTED_AT);
    }

    public static ReservationWithdrawnIE reservationWithdrawnEvent(String eventId, Instant withdrawnAt) {
        return new ReservationWithdrawnIE(eventId, SATURDAYS_RESERVATION_ID, AISYAH_ID, KEMAS_HOME_ID, withdrawnAt,
                REQUESTED_AT, withdrawnAt);
    }

    public static ReservationDeclinedIE reservationDeclinedEvent(String eventId, Instant declinedAt) {
        return new ReservationDeclinedIE(eventId, SATURDAYS_RESERVATION_ID, AISYAH_ID, KEMAS_HOME_ID, declinedAt,
                REQUESTED_AT, declinedAt);
    }

    public static ReservationExpiredIE reservationExpiredEvent(String eventId, Instant expiredAt) {
        return new ReservationExpiredIE(eventId, SATURDAYS_RESERVATION_ID, AISYAH_ID, KEMAS_HOME_ID, expiredAt,
                REQUESTED_AT, expiredAt);
    }

    /** A customer cancellation of Aisyah's reservation, free or not: Conversation reads only the times. */
    public static ReservationCancelledIE reservationCancelledEvent(String eventId, Instant cancelledAt) {
        return reservationCancelledEvent(eventId, cancelledAt, "CUSTOMER_CANCELLED");
    }

    public static ReservationCancelledIE reservationCancelledEvent(String eventId, Instant cancelledAt,
            String cause) {
        Money none = Money.myr(0);
        return new ReservationCancelledIE(eventId, SATURDAYS_RESERVATION_ID, AISYAH_ID, KEMAS_HOME_ID,
                cause, cancelledAt, REQUESTED_AT, HomaFixtures.CHECK_IN_PERIOD.start(),
                HomaFixtures.PROTECTION_WINDOW.end(), "settlement-1", none, HomaFixtures.TOTAL_RESERVATION_FEE, none,
                none, none, cancelledAt);
    }

    public static CustomerRegisteredIE customerRegisteredEvent(String eventId, String customerId) {
        return new CustomerRegisteredIE(eventId, customerId, HomaFixtures.AISYAH_CITY, "No. 12, Jalan Salak 3",
                "Guardhouse: say unit B-12-3", "ACTIVE", REQUESTED_AT);
    }

    public static CustomerVerifiedIE customerVerifiedEvent(String eventId, String customerId) {
        return new CustomerVerifiedIE(eventId, customerId, "person-" + customerId, REQUESTED_AT);
    }

    public static ProviderOpenedIE providerOpenedEvent(String eventId, String providerId, String ownerPersonId) {
        return new ProviderOpenedIE(eventId, providerId, ownerPersonId, REQUESTED_AT);
    }
}
