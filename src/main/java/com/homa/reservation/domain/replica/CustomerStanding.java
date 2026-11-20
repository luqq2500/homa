package com.homa.reservation.domain.replica;

import com.homa.reservation.domain.BookingRefusal;
import com.homa.reservation.domain.ServiceAddress;
import com.homa.reservation.domain.exception.BookingRefusedException;
import java.util.Objects;

/**
 * A registered customer as Reservation sees it, kept up to date from Customer's events. Only registered customers
 * have one (CUS-3); a suspended customer can't make reservations (CUS-5).
 */
public record CustomerStanding(String customerId, boolean suspended, ServiceAddress homeAddress) {

    public CustomerStanding {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(homeAddress, "homeAddress");
    }

    public boolean canReserve() {
        return !suspended;
    }

    public void requireCanReserve() {
        if (!canReserve()) {
            throw new BookingRefusedException(BookingRefusal.CUSTOMER_CANNOT_RESERVE);
        }
    }
}
