package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.ViewReservationRequest;
import com.homa.reservation.usecase.dto.ViewReservationResponse;

/** ROS-6: a party looks at a reservation and its roster. */
public interface ReservationViewer {

    ViewReservationResponse view(ViewReservationRequest request);
}
