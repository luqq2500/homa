package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.RespondToSmallerRosterRequest;
import com.homa.reservation.usecase.dto.RespondToSmallerRosterResponse;

/** ROS-8: the customer accepts or declines the smaller roster the provider offered. */
public interface SmallerRosterResponder {

    RespondToSmallerRosterResponse respond(RespondToSmallerRosterRequest request);
}
