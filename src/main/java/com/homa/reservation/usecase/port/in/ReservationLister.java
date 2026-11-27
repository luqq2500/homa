package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.ListMyReservationsRequest;
import com.homa.reservation.usecase.dto.ListMyReservationsResponse;

/** ROS-6: a party lists their reservations. */
public interface ReservationLister {

    ListMyReservationsResponse list(ListMyReservationsRequest request);
}
