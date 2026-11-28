package com.homa.conversation.usecase.port.out;

import com.homa.conversation.domain.ReservationChatWindow;
import com.homa.conversation.domain.ReservationId;
import java.util.Optional;

/**
 * The replica of each reservation's status and Protection Window end, from which a reservation's chat is open or
 * read-only (CHT-2, CHT-5). Infra rebuilds each with {@link ReservationChatWindow#restore}.
 */
public interface ReservationChatWindowRepository {

    Optional<ReservationChatWindow> findByReservationId(ReservationId reservationId);

    /** Adds the window or stores its changes. */
    void save(ReservationChatWindow window);
}
